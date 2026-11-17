package com.dramaease.tv.provider

import org.json.JSONObject
import java.net.URI

/**
 * A family-curated list of KissKH pages, read from a local JSON file (see docs/KISSKH_ASSESSMENT.md).
 * DramaEase does not scrape kisskh.do: the site is protected by an anti-bot challenge and offers no
 * documented API, so the app only stores links that a person chose to add, and only accepts kisskh.do links.
 */
data class LibraryEpisode(val number: Int, val title: String?, val pageUrl: String)

data class LibraryDrama(
    val key: String,
    val title: String,
    val description: String?,
    val posterUrl: String?,
    val pageUrl: String,
    val episodes: List<LibraryEpisode>,
)

object KissKhLibrary {
    const val SITE_HOST = "kisskh.do"
    const val SITE_NAME = "KissKH"
    const val HOME_URL = "https://kisskh.do"

    fun isAllowedPageUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return try {
            val uri = URI(url.trim())
            uri.scheme == "https" && (uri.host == SITE_HOST || uri.host?.endsWith(".$SITE_HOST") == true)
        } catch (_: Exception) {
            false
        }
    }

    private fun isHttpsUrl(url: String?): Boolean = try {
        url != null && URI(url.trim()).scheme == "https"
    } catch (_: Exception) {
        false
    }

    /** Returns valid entries only; malformed entries and links to other domains are skipped, never repaired. */
    fun parse(json: String): List<LibraryDrama> {
        val root = try { JSONObject(json) } catch (_: Exception) { return emptyList() }
        val arr = root.optJSONArray("dramas") ?: return emptyList()
        val result = mutableListOf<LibraryDrama>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val title = o.optString("title").trim()
            val page = o.optString("pageUrl").trim()
            if (title.isEmpty() || !isAllowedPageUrl(page)) continue
            val key = o.optString("id").trim().ifEmpty { (i + 1).toString() }
                .replace(Regex("[^A-Za-z0-9_-]"), "-")
            val eps = mutableListOf<LibraryEpisode>()
            val epArr = o.optJSONArray("episodes")
            if (epArr != null) {
                for (j in 0 until epArr.length()) {
                    val e = epArr.optJSONObject(j) ?: continue
                    val url = e.optString("pageUrl").trim()
                    val n = e.optInt("number", -1)
                    if (n < 1 || !isAllowedPageUrl(url)) continue
                    eps += LibraryEpisode(n, e.optString("title").trim().ifEmpty { null }, url)
                }
            }
            val poster = o.optString("posterUrl").trim().takeIf { isHttpsUrl(it) }
            result += LibraryDrama(
                key = key, title = title,
                description = o.optString("description").trim().ifEmpty { null },
                posterUrl = poster, pageUrl = page,
                episodes = eps.sortedBy { it.number }.distinctBy { it.number },
            )
        }
        return result.distinctBy { it.key }
    }
}
