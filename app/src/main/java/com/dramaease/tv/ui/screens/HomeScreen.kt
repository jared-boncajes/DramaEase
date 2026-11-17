package com.dramaease.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.dramaease.tv.data.AppSettings
import com.dramaease.tv.data.ContinueItem
import com.dramaease.tv.model.Drama
import com.dramaease.tv.ui.CatalogState
import com.dramaease.tv.ui.HomeViewModel
import com.dramaease.tv.ui.components.BigButton
import com.dramaease.tv.ui.components.ContinueCard
import com.dramaease.tv.ui.components.MessagePanel
import com.dramaease.tv.ui.components.PosterCard
import com.dramaease.tv.ui.components.SectionTitle
import com.dramaease.tv.ui.theme.Dimens
import com.dramaease.tv.ui.theme.DramaType
import com.dramaease.tv.ui.theme.LocalDramaColors
import com.dramaease.tv.ui.theme.LocalStrings

private sealed interface HomeRow {
    val key: String
    val title: String

    data class Continue(override val title: String, val items: List<ContinueItem>) : HomeRow { override val key = "cont" }
    data class Dramas(override val key: String, override val title: String, val items: List<Drama>) : HomeRow
}

private fun HomeRow.firstItemKey(): String? = when (this) {
    is HomeRow.Continue -> items.firstOrNull()?.let { "$key:${it.drama.id}" }
    is HomeRow.Dramas -> items.firstOrNull()?.let { "$key:${it.id}" }
}

@Composable
fun HomeScreen(
    vm: HomeViewModel,
    settings: AppSettings,
    onOpenDrama: (String) -> Unit,
    onPlay: (dramaId: String, episodeId: String) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
) {
    val s = LocalStrings.current
    val c = LocalDramaColors.current
    val cont by vm.continueItems.collectAsStateWithLifecycle()
    val favs by vm.favorites.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()
    val catalog by vm.catalog.collectAsStateWithLifecycle()
    val provider by vm.provider.collectAsStateWithLifecycle()

    val browse = (catalog as? CatalogState.Ready)?.dramas.orEmpty()
    // Order is fixed: Continue Watching, Favorites, Recently Watched, All Dramas. Empty rows are hidden.
    val rows = buildList {
        if (cont.isNotEmpty()) add(HomeRow.Continue(s.continueWatching, cont))
        if (favs.isNotEmpty()) add(HomeRow.Dramas("fav", s.favorites, favs))
        if (!settings.easyMode && recent.isNotEmpty()) add(HomeRow.Dramas("rec", s.recentlyWatched, recent))
        if (browse.isNotEmpty()) add(HomeRow.Dramas("all", s.browse, browse))
    }

    val restoreKey = vm.lastFocusKey ?: rows.firstOrNull()?.firstItemKey() ?: "search"
    val restoreRow = rows.indexOfFirst { r -> restoreKey.startsWith(r.key + ":") }.coerceAtLeast(0)
    // item 0 is the header, so rows start at index 1
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = if (restoreKey.contains(":")) restoreRow + 1 else 0)

    Box(Modifier.fillMaxSize().background(c.background)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = Dimens.safeHorizontal, end = Dimens.safeHorizontal, top = Dimens.safeVertical, bottom = 48.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            item(key = "header") {
                Header(
                    banner = when {
                        provider?.capabilities?.isSampleData == true -> s.demoBanner
                        provider != null -> s.kisskhBanner
                        else -> null
                    },
                    restoreKey = restoreKey, onFocus = { vm.lastFocusKey = it },
                    onSearch = onSearch, onSettings = onSettings,
                )
            }
            rows.forEach { row ->
                item(key = row.key) {
                    when (row) {
                        is HomeRow.Continue -> ItemsRow(
                            rowKey = row.key, title = row.title, items = row.items, itemId = { it.drama.id },
                            restoreKey = restoreKey, onFocus = { vm.lastFocusKey = it },
                        ) { item, mod ->
                            val label = when {
                                item.finishedEpisode -> s.upNext(item.episodeNumber + 1)
                                else -> s.episode(item.episodeNumber)
                            }
                            val sub = when {
                                item.finishedEpisode -> null
                                item.entry.progressConfirmed && item.entry.durationMs > 0 ->
                                    s.minutesLeft(((item.entry.durationMs - item.entry.positionMs) / 60_000L).toInt().coerceAtLeast(1))
                                !item.entry.progressConfirmed -> s.openedLabel
                                else -> null
                            }
                            ContinueCard(
                                drama = item.drama, episodeLabel = label, subLabel = sub, fraction = item.fraction,
                                onClick = {
                                    // Unfinished episode: resume directly (fewest presses). Finished: details picks the next one.
                                    if (item.finishedEpisode) onOpenDrama(item.drama.id) else onPlay(item.drama.id, item.episodeId)
                                },
                                modifier = mod,
                            )
                        }
                        is HomeRow.Dramas -> ItemsRow(
                            rowKey = row.key, title = row.title, items = row.items, itemId = { it.id },
                            restoreKey = restoreKey, onFocus = { vm.lastFocusKey = it },
                        ) { d, mod -> PosterCard(d, onClick = { onOpenDrama(d.id) }, modifier = mod) }
                    }
                }
            }
            if (browse.isEmpty()) {
                item(key = "status") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        when (catalog) {
                            CatalogState.Loading -> MessagePanel(s.loading)
                            CatalogState.Failed -> {
                                MessagePanel(s.loadFailed)
                                BigButton(s.tryAgain, onClick = vm::retry, primary = true)
                            }
                            is CatalogState.Ready ->
                                MessagePanel(if (provider?.capabilities?.isSampleData == true) s.emptyCatalogDemo else s.emptyCatalogKisskh)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(
    banner: String?, restoreKey: String, onFocus: (String) -> Unit, onSearch: () -> Unit, onSettings: () -> Unit,
) {
    val s = LocalStrings.current
    val c = LocalDramaColors.current
    val searchReq = remember { FocusRequester() }
    LaunchedEffect(restoreKey) {
        if (restoreKey == "search") runCatching { searchReq.requestFocus() }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(s.appName, style = DramaType.headline, color = c.accent)
            if (banner != null) Text(banner, style = DramaType.label, color = c.textDim)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BigButton(s.search, onClick = onSearch, modifier = Modifier.focusRequester(searchReq).onFocusChanged { if (it.hasFocus) onFocus("search") })
            BigButton(s.settings, onClick = onSettings, modifier = Modifier.onFocusChanged { if (it.hasFocus) onFocus("settings") })
        }
    }
}

/** A horizontal row that restores focus to the last focused card (by key) after coming back. */
@Composable
private fun <T> ItemsRow(
    rowKey: String,
    title: String,
    items: List<T>,
    itemId: (T) -> String,
    restoreKey: String,
    onFocus: (String) -> Unit,
    card: @Composable (T, Modifier) -> Unit,
) {
    val startIndex = remember(rowKey) { items.indexOfFirst { "$rowKey:${itemId(it)}" == restoreKey }.coerceAtLeast(0) }
    val state = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionTitle(title)
        LazyRow(
            state = state,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            // Room for the focus ring + 5% scale so nothing is clipped.
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp, horizontal = 10.dp),
        ) {
            items(items.size, key = { itemId(items[it]) }) { i ->
                val key = "$rowKey:${itemId(items[i])}"
                val req = remember { FocusRequester() }
                LaunchedEffect(restoreKey) { if (key == restoreKey) runCatching { req.requestFocus() } }
                card(items[i], Modifier.focusRequester(req).onFocusChanged { if (it.hasFocus) onFocus(key) })
            }
        }
    }
}
