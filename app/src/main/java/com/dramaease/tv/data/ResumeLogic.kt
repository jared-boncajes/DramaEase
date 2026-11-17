package com.dramaease.tv.data

import com.dramaease.tv.model.Episode

/** What the big primary button on a drama should do. Pure logic, unit tested. */
sealed interface ResumeTarget {
    val episode: Episode
    data class Start(override val episode: Episode) : ResumeTarget
    data class Continue(override val episode: Episode, val entry: WatchEntry) : ResumeTarget
    data class Next(override val episode: Episode) : ResumeTarget
    data class Again(override val episode: Episode) : ResumeTarget
}

object ResumeLogic {
    fun resolve(episodes: List<Episode>, lastEpisodeId: String?, entries: Map<String, WatchEntry>): ResumeTarget? {
        if (episodes.isEmpty()) return null
        val first = episodes.first()
        val last = episodes.firstOrNull { it.id == lastEpisodeId } ?: return ResumeTarget.Start(first)
        val entry = entries[last.id] ?: return ResumeTarget.Start(first)
        if (!entry.watched) return ResumeTarget.Continue(last, entry)
        val next = episodes.firstOrNull { it.number > last.number }
        return if (next != null) ResumeTarget.Next(next) else ResumeTarget.Again(first)
    }
}
