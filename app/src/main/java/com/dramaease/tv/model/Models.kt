package com.dramaease.tv.model

/** A drama series as shown in the app. Optional fields stay null when the provider does not supply them. */
data class Drama(
    val id: String,
    val title: String,
    val description: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val genres: List<String> = emptyList(),
    val episodeCount: Int? = null,
    val year: Int? = null,
)

data class Episode(
    val id: String,
    val dramaId: String,
    val number: Int,
    val title: String? = null,
    val thumbnailUrl: String? = null,
    val durationMs: Long? = null,
)

data class SubtitleTrack(
    val url: String,
    val language: String,
    val label: String,
    val mimeType: String,
)

/** How (and whether) an episode can be played. Providers must never invent stream URLs. */
sealed interface PlaybackOption {
    /** A stream the app is allowed to play itself with Media3. */
    data class NativeStream(
        val url: String,
        val subtitles: List<SubtitleTrack> = emptyList(),
        /** Where the right to play this comes from (e.g. licence). Shown in docs/About. */
        val rightsNote: String,
    ) : PlaybackOption

    /** Playback only happens on the original website, opened in an external browser. */
    data class ExternalWebsite(val pageUrl: String, val siteName: String) : PlaybackOption

    data class Unavailable(val reason: UnavailableReason) : PlaybackOption
}

enum class UnavailableReason { NOT_AVAILABLE, NOT_PERMITTED, NETWORK, UNKNOWN }
