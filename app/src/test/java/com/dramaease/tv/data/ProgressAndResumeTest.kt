package com.dramaease.tv.data

import com.dramaease.tv.model.Episode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressAndResumeTest {
    private fun entry(id: String, n: Int, pos: Long = 0, dur: Long = 0, watched: Boolean = false, confirmed: Boolean = true) =
        WatchEntry(id, "d", n, pos, dur, 0, watched, confirmed)

    private val eps = (1..3).map { Episode("e$it", "d", it) }

    @Test fun watchedThresholdIs95Percent() {
        assertFalse(ProgressRules.isWatched(940, 1000, false))
        assertTrue(ProgressRules.isWatched(950, 1000, false))
        assertTrue(ProgressRules.isWatched(10, 1000, ended = true))
        assertFalse(ProgressRules.isWatched(10, 0, false))
    }

    @Test fun resumeOnlyForConfirmedUnfinishedProgress() {
        assertEquals(60_000L, ProgressRules.resumePosition(entry("e1", 1, 60_000, 600_000)))
        assertEquals(0L, ProgressRules.resumePosition(entry("e1", 1, 2_000, 600_000)))
        assertEquals(0L, ProgressRules.resumePosition(entry("e1", 1, 60_000, 600_000, watched = true)))
        assertEquals(0L, ProgressRules.resumePosition(entry("e1", 1, 60_000, 600_000, confirmed = false)))
        assertEquals(0L, ProgressRules.resumePosition(null))
    }

    @Test fun noFakeProgressBarForExternalOpens() {
        assertNull(ProgressRules.fraction(entry("e1", 1, confirmed = false)))
        assertNull(ProgressRules.fraction(null))
        assertEquals(0.5f, ProgressRules.fraction(entry("e1", 1, 300, 600))!!, 0.001f)
        assertEquals(1f, ProgressRules.fraction(entry("e1", 1, 0, 600, watched = true))!!, 0.001f)
    }

    @Test fun targetStartsAtFirstEpisodeWhenNothingWatched() {
        assertTrue(ResumeLogic.resolve(eps, null, emptyMap()) is ResumeTarget.Start)
        assertNull(ResumeLogic.resolve(emptyList(), null, emptyMap()))
    }

    @Test fun targetContinuesUnfinishedEpisode() {
        val t = ResumeLogic.resolve(eps, "e2", mapOf("e2" to entry("e2", 2, 100_000, 600_000)))
        assertTrue(t is ResumeTarget.Continue)
        assertEquals("e2", t!!.episode.id)
    }

    @Test fun targetMovesToNextAfterFinishedAndLoopsAtEnd() {
        val next = ResumeLogic.resolve(eps, "e1", mapOf("e1" to entry("e1", 1, watched = true)))
        assertEquals("e2", (next as ResumeTarget.Next).episode.id)
        val again = ResumeLogic.resolve(eps, "e3", mapOf("e3" to entry("e3", 3, watched = true)))
        assertTrue(again is ResumeTarget.Again)
    }

    @Test fun externalOpenIsContinueNotWatched() {
        val t = ResumeLogic.resolve(eps, "e2", mapOf("e2" to entry("e2", 2, confirmed = false)))
        assertTrue(t is ResumeTarget.Continue)
    }
}
