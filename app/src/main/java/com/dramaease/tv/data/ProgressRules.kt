package com.dramaease.tv.data

/** Pure rules for what counts as "watched" and where to resume. Unit tested. */
object ProgressRules {
    const val WATCHED_FRACTION = 0.95
    const val MIN_RESUME_MS = 5_000L

    fun isWatched(positionMs: Long, durationMs: Long, ended: Boolean): Boolean =
        ended || (durationMs > 0 && positionMs >= (durationMs * WATCHED_FRACTION).toLong())

    /** Position to seek to when starting an episode; 0 means "from the beginning". */
    fun resumePosition(entry: WatchEntry?): Long {
        if (entry == null || !entry.progressConfirmed || entry.watched) return 0L
        return if (entry.positionMs >= MIN_RESUME_MS) entry.positionMs else 0L
    }

    /** 0f..1f, or null when no confirmed progress exists (so the UI shows no bar rather than a fake one). */
    fun fraction(entry: WatchEntry?): Float? {
        if (entry == null || !entry.progressConfirmed || entry.durationMs <= 0) return null
        if (entry.watched) return 1f
        return (entry.positionMs.toFloat() / entry.durationMs).coerceIn(0f, 1f)
    }
}
