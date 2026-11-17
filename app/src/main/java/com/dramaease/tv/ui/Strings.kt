package com.dramaease.tv.ui

import com.dramaease.tv.data.AppLanguage

/** All user-visible text, English + Filipino. Friendly wording, no technical terms. */
class Strings(private val lang: AppLanguage) {
    private fun t(en: String, fil: String) = if (lang == AppLanguage.FILIPINO) fil else en

    val appName = "DramaEase"
    val continueWatching get() = t("Continue Watching", "Ituloy ang Panonood")
    val favorites get() = t("My Favorites", "Mga Paborito Ko")
    val recentlyWatched get() = t("Recently Watched", "Kamakailang Pinanood")
    val browse get() = t("All Dramas", "Lahat ng Drama")
    val search get() = t("Search", "Maghanap")
    val settings get() = t("Settings", "Mga Setting")
    val pressOkToWatch get() = t("Press OK to Watch", "Pindutin ang OK para Manood")
    val startWatching get() = t("Start Watching", "Simulan ang Panonood")
    val chooseEpisode get() = t("Choose Episode", "Pumili ng Episode")
    val goBack get() = t("Go Back", "Bumalik")
    val tryAgain get() = t("Try Again", "Subukan Muli")
    val watchAgain get() = t("Watch Again", "Panoorin Muli")
    val addFavorite get() = t("Add to Favorites", "Idagdag sa Paborito")
    val removeFavorite get() = t("Remove from Favorites", "Alisin sa Paborito")
    val watchedLabel get() = t("Watched", "Napanood na")
    val openedLabel get() = t("Opened before", "Nabuksan na")
    val loading get() = t("Please wait…", "Sandali lang…")
    val nextEpisode get() = t("Next Episode", "Susunod na Episode")
    val previousEpisode get() = t("Previous", "Nauna")
    val backToEpisodes get() = t("Back to Episodes", "Bumalik sa mga Episode")
    val play get() = t("Play", "Play")
    val pause get() = t("Pause", "Pause")
    val back10 get() = t("Back 10 sec", "Balik 10 seg")
    val forward10 get() = t("Forward 10 sec", "Abante 10 seg")
    val subtitles get() = t("Subtitles", "Subtitle")
    val subtitlesOff get() = t("Off", "Patay")
    val subtitlesOn get() = t("On", "Bukas")
    val noSubtitles get() = t("No subtitles", "Walang subtitle")
    val noResults get() = t("No dramas found.", "Walang nakitang drama.")
    val typeToSearch get() = t("Press OK, then type a drama name", "Pindutin ang OK, saka i-type ang pangalan ng drama")
    val demoBanner get() = t("Practice mode — sample videos only", "Practice mode — sample na video lang")
    val kisskhBanner get() = t("Your saved KissKH list", "Ang naka-save mong listahan sa KissKH")
    val emptyCatalogDemo get() = t("Nothing to show yet.", "Wala pang maipapakita.")
    val emptyCatalogKisskh
        get() = t(
            "Your list is empty. Ask a family member to add your dramas.",
            "Walang laman ang listahan mo. Magpatulong sa kapamilya para idagdag ang mga drama mo.",
        )
    val loadFailed get() = t("We couldn't load the dramas. Check the internet and try again.", "Hindi ma-load ang mga drama. Tingnan ang internet at subukan muli.")
    val playFailed get() = t("This video can't play right now. Please try again.", "Hindi ma-play ang video ngayon. Subukan muli.")
    val unavailable get() = t("This episode isn't available.", "Hindi available ang episode na ito.")
    val noEpisodes get() = t("Episodes aren't available for this drama.", "Walang available na episode para sa drama na ito.")
    val openWebsite get() = t("Open the Website", "Buksan ang Website")
    val externalExplain
        get() = t(
            "This episode plays on the KissKH website, not inside DramaEase. Pause, rewind and subtitles use the website's own buttons.",
            "Sa website ng KissKH pinapanood ang episode na ito, hindi sa DramaEase. Ang pause, rewind at subtitle ay gamit ang sariling button ng website.",
        )
    val noBrowser
        get() = t(
            "This TV has no web browser installed. Ask a family member to install one.",
            "Walang web browser ang TV na ito. Magpatulong sa kapamilya para mag-install.",
        )

    // settings
    val easyMode get() = t("Easy Mode", "Easy Mode")
    val textSize get() = t("Text Size", "Laki ng Letra")
    val highContrast get() = t("High Contrast", "Matingkad na Kulay")
    val language get() = t("Language", "Wika")
    val subtitleSize get() = t("Subtitle Size", "Laki ng Subtitle")
    val contentSource get() = t("Where dramas come from", "Pinagmumulan ng mga drama")
    val on get() = t("On", "Bukas")
    val off get() = t("Off", "Patay")
    val sizeNormal get() = t("Normal", "Normal")
    val sizeLarge get() = t("Large", "Malaki")
    val sizeExtraLarge get() = t("Extra Large", "Napakalaki")
    val langName get() = if (lang == AppLanguage.FILIPINO) "Filipino" else "English"
    val sourceDemo get() = t("Practice videos", "Practice na video")
    val sourceKisskh get() = t("KissKH list (opens website)", "Listahan ng KissKH (bubukas ang website)")
    val pressOkToChange get() = t("Press OK to change", "Pindutin ang OK para palitan")

    fun episode(n: Int) = t("Episode $n", "Episode $n")
    fun continueEpisode(n: Int) = t("Continue Episode $n", "Ituloy ang Episode $n")
    fun watchEpisode(n: Int) = t("Watch Episode $n", "Panoorin ang Episode $n")
    fun episodeCount(n: Int) = t("$n episodes", "$n na episode")
    fun upNext(n: Int) = t("Up next: Episode $n", "Susunod: Episode $n")
    fun minutesLeft(min: Int) = t("$min min left", "$min min na lang")
    fun searchResultsFor(q: String) = t("Results for “$q”", "Resulta para sa “$q”")
}
