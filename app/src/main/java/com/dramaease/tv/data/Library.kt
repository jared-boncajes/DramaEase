package com.dramaease.tv.data

import com.dramaease.tv.model.Drama
import com.dramaease.tv.model.Episode
import com.dramaease.tv.provider.ContentSource
import com.dramaease.tv.provider.DramaContentProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** A "Continue Watching" card. [progressConfirmed] false means the episode page was only opened. */
data class ContinueItem(
    val drama: Drama,
    val episodeId: String,
    val episodeNumber: Int,
    val entry: WatchEntry,
) {
    val fraction: Float? get() = ProgressRules.fraction(entry)
    val finishedEpisode: Boolean get() = entry.watched
}

/** Single access point for provider data + local user data. */
class Library(
    private val dao: UserDao,
    private val settings: SettingsRepository,
    private val providers: Map<ContentSource, DramaContentProvider>,
    private val scope: CoroutineScope,
) {
    val provider: Flow<DramaContentProvider> =
        settings.settings.map { providers.getValue(it.source) }.distinctUntilChanged()

    suspend fun currentProvider(): DramaContentProvider = provider.first()

    private fun prefix() = provider.map { it.source.idPrefix }

    // ---- catalog cache so rows never trigger repeated provider calls ----
    private val catalogCache = HashMap<ContentSource, List<Drama>>()

    suspend fun catalog(forceRefresh: Boolean = false): List<Drama> {
        val p = currentProvider()
        if (!forceRefresh) catalogCache[p.source]?.let { return it }
        return p.getCatalog().also { catalogCache[p.source] = it }
    }

    // ---- user data flows ----
    fun favorites(): Flow<List<Drama>> = prefix().flatMapLatestCompat { dao.favorites(it) }
        .map { list -> list.map { Drama(it.dramaId, it.title, posterUrl = it.posterUrl) } }

    fun recent(): Flow<List<Drama>> = prefix().flatMapLatestCompat { dao.history(it, 20) }
        .map { list -> list.map { it.toDrama() } }

    fun continueWatching(): Flow<List<ContinueItem>> = prefix().flatMapLatestCompat { pre ->
        combine(dao.history(pre, 20), dao.allEntries(pre)) { history, entries ->
            val byEpisode = entries.associateBy { it.episodeId }
            history.mapNotNull { h ->
                val epId = h.lastEpisodeId ?: return@mapNotNull null
                val entry = byEpisode[epId] ?: return@mapNotNull null
                val total = h.episodeCount
                // A fully finished series has nothing left to continue.
                if (entry.watched && total != null && entry.episodeNumber >= total) return@mapNotNull null
                ContinueItem(h.toDrama(), epId, entry.episodeNumber, entry)
            }
        }
    }

    fun isFavorite(dramaId: String): Flow<Boolean> = dao.isFavorite(dramaId).map { it > 0 }
    fun entriesFor(dramaId: String): Flow<List<WatchEntry>> = dao.entriesFor(dramaId)
    fun historyFor(dramaId: String): Flow<DramaHistory?> = dao.historyFor(dramaId)

    // ---- writes run in an app-wide scope so they finish even if a screen closes ----
    fun setFavorite(drama: Drama, favorite: Boolean) {
        scope.launch {
            if (favorite) dao.upsertFavorite(FavoriteEntity(drama.id, drama.title, drama.posterUrl, now()))
            else dao.removeFavorite(drama.id)
        }
    }

    fun recordDramaOpened(drama: Drama) {
        scope.launch {
            val old = dao.historyForOnce(drama.id)
            dao.upsertHistory(
                DramaHistory(
                    dramaId = drama.id, title = drama.title, posterUrl = drama.posterUrl,
                    episodeCount = drama.episodeCount ?: old?.episodeCount,
                    lastOpenedAt = now(),
                    lastEpisodeId = old?.lastEpisodeId, lastEpisodeNumber = old?.lastEpisodeNumber,
                ),
            )
        }
    }

    /** Native playback progress: this is the ONLY place that may set watched/progressConfirmed to true. */
    fun recordPlayback(drama: Drama, episode: Episode, positionMs: Long, durationMs: Long, ended: Boolean) {
        scope.launch {
            val watched = ProgressRules.isWatched(positionMs, durationMs, ended)
            dao.upsertEntry(
                WatchEntry(
                    episodeId = episode.id, dramaId = episode.dramaId, episodeNumber = episode.number,
                    positionMs = if (watched) 0L else positionMs.coerceAtLeast(0L),
                    durationMs = durationMs.coerceAtLeast(0L), lastWatchedAt = now(),
                    watched = watched, progressConfirmed = true,
                ),
            )
            touchHistory(drama, episode)
        }
    }

    /** External website: we only know the page was opened. Never marks anything as watched. */
    fun recordOpenedExternally(drama: Drama, episode: Episode) {
        scope.launch {
            val old = dao.entry(episode.id)
            dao.upsertEntry(
                old?.copy(lastWatchedAt = now()) ?: WatchEntry(
                    episodeId = episode.id, dramaId = episode.dramaId, episodeNumber = episode.number,
                    positionMs = 0, durationMs = 0, lastWatchedAt = now(),
                    watched = false, progressConfirmed = false,
                ),
            )
            touchHistory(drama, episode)
        }
    }

    suspend fun entry(episodeId: String): WatchEntry? = dao.entry(episodeId)

    private suspend fun touchHistory(drama: Drama, episode: Episode) {
        val old = dao.historyForOnce(drama.id)
        dao.upsertHistory(
            DramaHistory(
                dramaId = drama.id, title = drama.title, posterUrl = drama.posterUrl ?: old?.posterUrl,
                episodeCount = drama.episodeCount ?: old?.episodeCount,
                lastOpenedAt = now(), lastEpisodeId = episode.id, lastEpisodeNumber = episode.number,
            ),
        )
    }

    private fun now() = System.currentTimeMillis()

    private fun DramaHistory.toDrama() = Drama(dramaId, title, posterUrl = posterUrl, episodeCount = episodeCount)
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
private fun <T, R> Flow<T>.flatMapLatestCompat(transform: (T) -> Flow<R>): Flow<R> =
    this.flatMapLatest { transform(it) }
