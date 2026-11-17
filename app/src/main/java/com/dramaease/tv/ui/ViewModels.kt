package com.dramaease.tv.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dramaease.tv.AppContainer
import com.dramaease.tv.data.ContinueItem
import com.dramaease.tv.data.Library
import com.dramaease.tv.data.ProgressRules
import com.dramaease.tv.data.ResumeLogic
import com.dramaease.tv.data.ResumeTarget
import com.dramaease.tv.data.WatchEntry
import com.dramaease.tv.model.Drama
import com.dramaease.tv.model.Episode
import com.dramaease.tv.model.PlaybackOption
import com.dramaease.tv.provider.DramaContentProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VmFactory(private val container: AppContainer, private val args: List<String> = emptyList()) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        HomeViewModel::class.java -> HomeViewModel(container.library)
        DramaViewModel::class.java -> DramaViewModel(container.library, args[0])
        PlayerViewModel::class.java -> PlayerViewModel(container.library, args[0], args[1])
        SearchViewModel::class.java -> SearchViewModel(container.library)
        else -> error("Unknown ViewModel")
    } as T
}

// ---------------------------------------------------------------- Home

sealed interface CatalogState {
    data object Loading : CatalogState
    data object Failed : CatalogState
    data class Ready(val dramas: List<Drama>) : CatalogState
}

class HomeViewModel(private val library: Library) : ViewModel() {
    val continueItems: StateFlow<List<ContinueItem>> =
        library.continueWatching().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val favorites: StateFlow<List<Drama>> =
        library.favorites().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val recent: StateFlow<List<Drama>> =
        library.recent().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val catalog = MutableStateFlow<CatalogState>(CatalogState.Loading)
    val provider = MutableStateFlow<DramaContentProvider?>(null)

    /** Key of the last focused card, restored when coming back to Home. */
    var lastFocusKey: String? = null

    init {
        viewModelScope.launch { library.provider.collect { provider.value = it; load(false) } }
    }

    fun retry() { viewModelScope.launch { load(true) } }

    private suspend fun load(force: Boolean) {
        catalog.value = CatalogState.Loading
        catalog.value = try { CatalogState.Ready(library.catalog(force)) } catch (_: Exception) { CatalogState.Failed }
    }
}

// ---------------------------------------------------------------- Drama details + episodes

data class DramaUi(
    val loading: Boolean = true,
    val drama: Drama? = null,
    val episodes: List<Episode> = emptyList(),
    val entries: Map<String, WatchEntry> = emptyMap(),
    val favorite: Boolean = false,
    val target: ResumeTarget? = null,
)

class DramaViewModel(private val library: Library, private val dramaId: String) : ViewModel() {
    private data class Loaded(val drama: Drama?, val episodes: List<Episode>)

    private val loaded = MutableStateFlow<Loaded?>(null)

    val ui: StateFlow<DramaUi> = combine(
        loaded, library.entriesFor(dramaId), library.isFavorite(dramaId), library.historyFor(dramaId),
    ) { l, entries, fav, history ->
        if (l == null) DramaUi()
        else {
            val map = entries.associateBy { it.episodeId }
            DramaUi(
                loading = false, drama = l.drama, episodes = l.episodes, entries = map, favorite = fav,
                target = ResumeLogic.resolve(l.episodes, history?.lastEpisodeId, map),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DramaUi())

    init {
        viewModelScope.launch {
            val p = library.currentProvider()
            val drama = try { p.getDramaDetails(dramaId) } catch (_: Exception) { null }
            val episodes = try { p.getEpisodes(dramaId) } catch (_: Exception) { emptyList() }
            drama?.let { library.recordDramaOpened(it.copy(episodeCount = it.episodeCount ?: episodes.size.takeIf { n -> n > 0 })) }
            loaded.value = Loaded(drama, episodes)
        }
    }

    fun toggleFavorite() {
        val d = ui.value.drama ?: return
        library.setFavorite(d, !ui.value.favorite)
    }
}

// ---------------------------------------------------------------- Search

class SearchViewModel(private val library: Library) : ViewModel() {
    data class State(val query: String = "", val results: List<Drama> = emptyList(), val searched: Boolean = false, val failed: Boolean = false)

    val state = MutableStateFlow(State())

    fun search(query: String) {
        val q = query.trim()
        state.update { it.copy(query = query) }
        viewModelScope.launch {
            if (q.isEmpty()) { state.update { State(query = query) }; return@launch }
            val r = try { library.currentProvider().searchDramas(q) } catch (_: Exception) { null }
            // Ignore stale answers if the user typed something else meanwhile.
            if (state.value.query.trim() == q) {
                state.update { it.copy(results = r ?: emptyList(), searched = true, failed = r == null) }
            }
        }
    }
}

// ---------------------------------------------------------------- Player

sealed interface PlayerUi {
    data object Loading : PlayerUi
    data object Unavailable : PlayerUi
    data class Native(
        val drama: Drama, val episode: Episode, val option: PlaybackOption.NativeStream,
        val resumeMs: Long, val previous: Episode?, val next: Episode?,
    ) : PlayerUi
    data class External(val drama: Drama, val episode: Episode, val option: PlaybackOption.ExternalWebsite) : PlayerUi
}

class PlayerViewModel(private val library: Library, private val dramaId: String, private val episodeId: String) : ViewModel() {
    val ui = MutableStateFlow<PlayerUi>(PlayerUi.Loading)

    init { load() }

    fun load() {
        ui.value = PlayerUi.Loading
        viewModelScope.launch {
            ui.value = try {
                val p = library.currentProvider()
                val drama = p.getDramaDetails(dramaId)
                val episodes = p.getEpisodes(dramaId)
                val episode = episodes.firstOrNull { it.id == episodeId }
                if (drama == null || episode == null) PlayerUi.Unavailable
                else when (val option = p.getPlaybackOption(episodeId)) {
                    is PlaybackOption.NativeStream -> {
                        val i = episodes.indexOf(episode)
                        PlayerUi.Native(
                            drama, episode, option,
                            ProgressRules.resumePosition(library.entry(episodeId)),
                            episodes.getOrNull(i - 1), episodes.getOrNull(i + 1),
                        )
                    }
                    is PlaybackOption.ExternalWebsite -> PlayerUi.External(drama, episode, option)
                    is PlaybackOption.Unavailable -> PlayerUi.Unavailable
                }
            } catch (_: Exception) {
                PlayerUi.Unavailable
            }
        }
    }

    fun saveProgress(positionMs: Long, durationMs: Long, ended: Boolean) {
        val s = ui.value as? PlayerUi.Native ?: return
        library.recordPlayback(s.drama, s.episode, positionMs, durationMs, ended)
    }

    fun markOpenedExternally() {
        val s = ui.value as? PlayerUi.External ?: return
        library.recordOpenedExternally(s.drama, s.episode)
    }
}
