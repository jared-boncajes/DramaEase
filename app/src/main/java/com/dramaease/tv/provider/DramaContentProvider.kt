package com.dramaease.tv.provider

import com.dramaease.tv.model.Drama
import com.dramaease.tv.model.Episode
import com.dramaease.tv.model.PlaybackOption

enum class ContentSource(val id: String) {
    DEMO("demo"),
    KISSKH("kisskh");

    /** Every drama/episode id of this source starts with this prefix, so local data never mixes between sources. */
    val idPrefix: String get() = "$id:"
}

/** What a provider can honestly do. The UI adapts to this instead of pretending. */
data class ProviderCapabilities(
    val hasCatalog: Boolean,
    val hasSearch: Boolean,
    val nativePlayback: Boolean,
    val isSampleData: Boolean,
)

interface DramaContentProvider {
    val source: ContentSource
    val capabilities: ProviderCapabilities

    suspend fun getCatalog(): List<Drama>
    suspend fun searchDramas(query: String): List<Drama>
    suspend fun getDramaDetails(id: String): Drama?
    suspend fun getEpisodes(dramaId: String): List<Episode>
    suspend fun getPlaybackOption(episodeId: String): PlaybackOption
}
