package com.dramaease.tv.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One row per episode that was played (confirmed) or merely opened on an external site (not confirmed). */
@Entity(tableName = "watch_entries")
data class WatchEntry(
    @PrimaryKey val episodeId: String,
    val dramaId: String,
    val episodeNumber: Int,
    val positionMs: Long,
    val durationMs: Long,
    val lastWatchedAt: Long,
    /** True only when finished in the native player (>=95% or ended). Never set for external pages. */
    val watched: Boolean,
    /** True only when position/duration came from the native player. False = "page was opened". */
    val progressConfirmed: Boolean,
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val dramaId: String,
    val title: String,
    val posterUrl: String?,
    val addedAt: Long,
)

/** A drama the user opened. Stores a small snapshot so Home needs no network request. */
@Entity(tableName = "drama_history")
data class DramaHistory(
    @PrimaryKey val dramaId: String,
    val title: String,
    val posterUrl: String?,
    val episodeCount: Int?,
    val lastOpenedAt: Long,
    val lastEpisodeId: String?,
    val lastEpisodeNumber: Int?,
)
