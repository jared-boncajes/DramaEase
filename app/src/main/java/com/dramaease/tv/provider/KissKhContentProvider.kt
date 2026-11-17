package com.dramaease.tv.provider

import com.dramaease.tv.model.Drama
import com.dramaease.tv.model.Episode
import com.dramaease.tv.model.PlaybackOption
import com.dramaease.tv.model.UnavailableReason
import java.io.File

/**
 * Adapter for https://kisskh.do — implements ONLY what was verified as permitted (see docs/KISSKH_ASSESSMENT.md):
 *  - no automated catalog/search/episode/stream access (anti-bot challenge, no documented API) -> not implemented;
 *  - a locally stored, person-curated list of kisskh.do links is shown in the native interface;
 *  - playback is always "open the original website"; the app never extracts or plays KissKH streams itself.
 */
class KissKhContentProvider(private val libraryFile: File?) : DramaContentProvider {
    override val source = ContentSource.KISSKH
    override val capabilities = ProviderCapabilities(
        hasCatalog = true, hasSearch = true, nativePlayback = false, isSampleData = false,
    )

    private var cachedStamp = -1L
    private var cached: List<LibraryDrama> = emptyList()

    private fun library(): List<LibraryDrama> {
        val f = libraryFile
        if (f == null || !f.isFile) { cachedStamp = -1L; cached = emptyList(); return cached }
        val stamp = f.lastModified() xor f.length()
        if (stamp != cachedStamp) {
            cached = try { KissKhLibrary.parse(f.readText()) } catch (_: Exception) { emptyList() }
            cachedStamp = stamp
        }
        return cached
    }

    private fun toDrama(d: LibraryDrama) = Drama(
        id = "kisskh:${d.key}", title = d.title, description = d.description, posterUrl = d.posterUrl,
        episodeCount = d.episodes.size.takeIf { it > 0 },
    )

    private fun find(dramaId: String) = library().firstOrNull { "kisskh:${it.key}" == dramaId }

    override suspend fun getCatalog(): List<Drama> = library().map(::toDrama)

    override suspend fun searchDramas(query: String): List<Drama> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        return library().filter { it.title.contains(q, ignoreCase = true) }.map(::toDrama)
    }

    override suspend fun getDramaDetails(id: String): Drama? = find(id)?.let(::toDrama)

    override suspend fun getEpisodes(dramaId: String): List<Episode> {
        val d = find(dramaId) ?: return emptyList()
        // A drama saved without episode links gets one entry that opens its page; it is not a real "Episode 1" title.
        if (d.episodes.isEmpty()) {
            return listOf(Episode(id = "$dramaId:page", dramaId = dramaId, number = 1, title = null))
        }
        return d.episodes.map { Episode(id = "$dramaId:ep${it.number}", dramaId = dramaId, number = it.number, title = it.title) }
    }

    override suspend fun getPlaybackOption(episodeId: String): PlaybackOption {
        val dramaId = episodeId.substringBeforeLast(":")
        val d = find(dramaId) ?: return PlaybackOption.Unavailable(UnavailableReason.NOT_AVAILABLE)
        val tail = episodeId.substringAfterLast(":")
        val url = if (tail == "page") d.pageUrl
        else d.episodes.firstOrNull { "ep${it.number}" == tail }?.pageUrl
        return if (url != null && KissKhLibrary.isAllowedPageUrl(url)) {
            PlaybackOption.ExternalWebsite(url, KissKhLibrary.SITE_NAME)
        } else {
            PlaybackOption.Unavailable(UnavailableReason.NOT_AVAILABLE)
        }
    }
}
