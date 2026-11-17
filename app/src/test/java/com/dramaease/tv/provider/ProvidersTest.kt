package com.dramaease.tv.provider

import com.dramaease.tv.model.PlaybackOption
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProvidersTest {
    @Test fun demoProviderHasCatalogEpisodesAndNativePlayback() = runTest {
        val p = DemoContentProvider()
        assertTrue(p.capabilities.isSampleData)
        val catalog = p.getCatalog()
        assertTrue(catalog.size >= 5)
        val eps = p.getEpisodes(catalog[0].id)
        assertEquals(catalog[0].episodeCount, eps.size)
        val opt = p.getPlaybackOption(eps[0].id)
        assertTrue(opt is PlaybackOption.NativeStream)
        assertEquals(1, p.searchDramas("moon").size)
        assertTrue(p.searchDramas("   ").isEmpty())
    }

    @Test fun kisskhWithoutLibraryFileIsEmptyAndNeverInventsStreams() = runTest {
        val p = KissKhContentProvider(File("/nonexistent/kisskh_library.json"))
        assertFalse(p.capabilities.nativePlayback)
        assertTrue(p.getCatalog().isEmpty())
        assertTrue(p.getEpisodes("kisskh:x").isEmpty())
        assertTrue(p.getPlaybackOption("kisskh:x:ep1") is PlaybackOption.Unavailable)
    }

    @Test fun kisskhLibraryAcceptsOnlyKisskhLinksAndPlaysExternally() = runTest {
        val f = File.createTempFile("lib", ".json").apply {
            deleteOnExit()
            writeText(
                """{"dramas":[
                  {"id":"a","title":"Good","pageUrl":"https://kisskh.do/Drama/A",
                   "episodes":[{"number":2,"pageUrl":"https://kisskh.do/Drama/A/Episode-2"},
                               {"number":1,"pageUrl":"https://kisskh.do/Drama/A/Episode-1"},
                               {"number":3,"pageUrl":"https://evil.example/ep3"}]},
                  {"id":"b","title":"Other site","pageUrl":"https://example.com/x"},
                  {"id":"c","title":"No episodes","pageUrl":"https://kisskh.do/Drama/C"}
                ]}""",
            )
        }
        val p = KissKhContentProvider(f)
        val catalog = p.getCatalog()
        assertEquals(listOf("Good", "No episodes"), catalog.map { it.title })
        val eps = p.getEpisodes("kisskh:a")
        assertEquals(listOf(1, 2), eps.map { it.number }) // sorted, foreign-domain episode dropped
        val opt = p.getPlaybackOption(eps[0].id)
        assertTrue(opt is PlaybackOption.ExternalWebsite)
        assertEquals("https://kisskh.do/Drama/A/Episode-1", (opt as PlaybackOption.ExternalWebsite).pageUrl)
        // drama without episode links -> one entry that opens the drama page
        val single = p.getEpisodes("kisskh:c")
        assertEquals(1, single.size)
        assertEquals("https://kisskh.do/Drama/C", (p.getPlaybackOption(single[0].id) as PlaybackOption.ExternalWebsite).pageUrl)
    }

    @Test fun urlValidation() {
        assertTrue(KissKhLibrary.isAllowedPageUrl("https://kisskh.do/Drama/X"))
        assertTrue(KissKhLibrary.isAllowedPageUrl("https://www.kisskh.do/x"))
        assertFalse(KissKhLibrary.isAllowedPageUrl("http://kisskh.do/x"))
        assertFalse(KissKhLibrary.isAllowedPageUrl("https://kisskh.do.evil.com/x"))
        assertFalse(KissKhLibrary.isAllowedPageUrl("https://notkisskh.do/x"))
        assertFalse(KissKhLibrary.isAllowedPageUrl(""))
        assertTrue(KissKhLibrary.parse("not json").isEmpty())
    }
}
