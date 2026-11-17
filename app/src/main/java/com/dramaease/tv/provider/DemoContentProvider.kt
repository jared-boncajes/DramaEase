package com.dramaease.tv.provider

import com.dramaease.tv.model.Drama
import com.dramaease.tv.model.Episode
import com.dramaease.tv.model.PlaybackOption
import com.dramaease.tv.model.SubtitleTrack
import com.dramaease.tv.model.UnavailableReason

/**
 * Development/practice content. Titles are fictional, posters are drawn by the app (no images shipped),
 * and every episode plays an openly licensed Blender Foundation film (CC-BY 3.0) so the player,
 * progress saving and focus behaviour can be tested. This is NOT a KissKH integration.
 */
class DemoContentProvider : DramaContentProvider {
    override val source = ContentSource.DEMO
    override val capabilities = ProviderCapabilities(
        hasCatalog = true, hasSearch = true, nativePlayback = true, isSampleData = true,
    )

    private data class Seed(val key: String, val title: String, val description: String, val genres: List<String>, val episodes: Int, val year: Int)

    private val seeds = listOf(
        Seed("1", "Moonlight Garden", "A florist inherits a quiet garden and the secrets buried beneath its roses.", listOf("Romance", "Family"), 16, 2022),
        Seed("2", "The Lantern Keeper", "A village lamplighter guards a promise made across three generations.", listOf("Historical", "Drama"), 20, 2021),
        Seed("3", "Seoul Spring Letters", "Two pen pals discover they live on the same street.", listOf("Romance", "Comedy"), 12, 2023),
        Seed("4", "Harbor of Second Chances", "A retired chef reopens her late husband's seaside restaurant.", listOf("Family", "Slice of life"), 24, 2020),
        Seed("5", "Paper Kites", "Childhood friends reunite to rescue their town's yearly festival.", listOf("Comedy", "Friendship"), 10, 2024),
        Seed("6", "Winter Station Lane", "A stationmaster's last winter brings travelers with stories to tell.", listOf("Drama", "Mystery"), 18, 2019),
    )

    private val videos = listOf(
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
    )

    private val dramas = seeds.map {
        Drama(
            id = "demo:${it.key}", title = it.title, description = it.description,
            genres = it.genres, episodeCount = it.episodes, year = it.year,
        )
    }

    override suspend fun getCatalog(): List<Drama> = dramas

    override suspend fun searchDramas(query: String): List<Drama> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        return dramas.filter { it.title.contains(q, ignoreCase = true) }
    }

    override suspend fun getDramaDetails(id: String): Drama? = dramas.firstOrNull { it.id == id }

    override suspend fun getEpisodes(dramaId: String): List<Episode> {
        val drama = getDramaDetails(dramaId) ?: return emptyList()
        return (1..(drama.episodeCount ?: 0)).map { n ->
            Episode(id = "$dramaId:ep$n", dramaId = dramaId, number = n, title = "Sample episode $n")
        }
    }

    override suspend fun getPlaybackOption(episodeId: String): PlaybackOption {
        val n = episodeId.substringAfterLast(":ep", "").toIntOrNull()
            ?: return PlaybackOption.Unavailable(UnavailableReason.NOT_AVAILABLE)
        return PlaybackOption.NativeStream(
            url = videos[n % videos.size],
            subtitles = listOf(
                SubtitleTrack(
                    url = DEMO_SUBTITLE_URI, language = "en", label = "English (sample)", mimeType = "text/vtt",
                ),
            ),
            rightsNote = "Blender Foundation films, CC-BY 3.0 (sample/test content only).",
        )
    }

    companion object {
        const val DEMO_SUBTITLE_URI = "android.resource://com.dramaease.tv/raw/demo_subtitles"
    }
}
