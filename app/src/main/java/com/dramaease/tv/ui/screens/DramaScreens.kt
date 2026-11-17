package com.dramaease.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.dramaease.tv.data.ProgressRules
import com.dramaease.tv.data.ResumeTarget
import com.dramaease.tv.data.WatchEntry
import com.dramaease.tv.model.Episode
import com.dramaease.tv.ui.DramaUi
import com.dramaease.tv.ui.DramaViewModel
import com.dramaease.tv.ui.components.BigButton
import com.dramaease.tv.ui.components.FocusCard
import com.dramaease.tv.ui.components.MessagePanel
import com.dramaease.tv.ui.components.PosterArt
import com.dramaease.tv.ui.components.ProgressBar
import com.dramaease.tv.ui.components.placeholderBrush
import com.dramaease.tv.ui.theme.Dimens
import com.dramaease.tv.ui.theme.DramaType
import com.dramaease.tv.ui.theme.LocalDramaColors
import com.dramaease.tv.ui.theme.LocalStrings

@Composable
fun DramaDetailsScreen(
    vm: DramaViewModel,
    onPlay: (episodeId: String) -> Unit,
    onChooseEpisode: () -> Unit,
) {
    val s = LocalStrings.current
    val c = LocalDramaColors.current
    val ui by vm.ui.collectAsStateWithLifecycle()
    val primaryReq = remember { FocusRequester() }
    val favReq = remember { FocusRequester() }
    // The primary action gets focus automatically (as soon as data is ready and whenever the target changes).
    LaunchedEffect(ui.loading, ui.target) {
        if (!ui.loading) runCatching { if (ui.target != null) primaryReq.requestFocus() else favReq.requestFocus() }
    }

    Box(Modifier.fillMaxSize().background(c.background).padding(horizontal = Dimens.safeHorizontal, vertical = Dimens.safeVertical)) {
        val drama = ui.drama
        when {
            ui.loading -> MessagePanel(s.loading)
            drama == null -> Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                MessagePanel(s.unavailable)
            }
            else -> Row(horizontalArrangement = Arrangement.spacedBy(48.dp), verticalAlignment = Alignment.Top) {
                PosterArt(drama, Modifier.size(width = 300.dp, height = 430.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text(drama.title, style = DramaType.headline, color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    // Only metadata the provider actually supplied is shown.
                    val meta = buildList {
                        if (drama.genres.isNotEmpty()) add(drama.genres.joinToString(", "))
                        val n = drama.episodeCount ?: ui.episodes.size.takeIf { it > 0 }
                        if (n != null) add(s.episodeCount(n))
                        drama.year?.let { add(it.toString()) }
                    }
                    if (meta.isNotEmpty()) Text(meta.joinToString("  •  "), style = DramaType.label, color = c.textDim)
                    drama.description?.let {
                        Text(it, style = DramaType.body, color = c.text, maxLines = 4, overflow = TextOverflow.Ellipsis)
                    }
                    Box(Modifier.height(8.dp))
                    val target = ui.target
                    if (target != null) {
                        BigButton(
                            primaryLabel(target), onClick = { onPlay(target.episode.id) }, primary = true,
                            modifier = Modifier.fillMaxWidth(0.8f).focusRequester(primaryReq),
                        )
                        Text(s.pressOkToWatch, style = DramaType.label, color = c.textDim)
                    } else {
                        MessagePanel(s.noEpisodes)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        if (ui.episodes.size > 1) BigButton(s.chooseEpisode, onClick = onChooseEpisode)
                        BigButton(
                            if (ui.favorite) s.removeFavorite else s.addFavorite, onClick = vm::toggleFavorite,
                            modifier = Modifier.focusRequester(favReq),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun primaryLabel(t: ResumeTarget): String {
    val s = LocalStrings.current
    return when (t) {
        is ResumeTarget.Start -> s.startWatching
        is ResumeTarget.Continue -> s.continueEpisode(t.episode.number)
        is ResumeTarget.Next -> s.watchEpisode(t.episode.number)
        is ResumeTarget.Again -> s.watchAgain
    }
}

private const val GRID_COLUMNS = 4

@Composable
fun EpisodesScreen(vm: DramaViewModel, onPlay: (episodeId: String) -> Unit) {
    val s = LocalStrings.current
    val c = LocalDramaColors.current
    val ui by vm.ui.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().background(c.background).padding(horizontal = Dimens.safeHorizontal, vertical = Dimens.safeVertical)) {
        Text(ui.drama?.title ?: "", style = DramaType.headline, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(s.chooseEpisode, style = DramaType.title, color = c.accent, modifier = Modifier.padding(bottom = 16.dp))
        when {
            ui.loading -> MessagePanel(s.loading)
            ui.episodes.isEmpty() -> MessagePanel(s.noEpisodes)
            else -> EpisodeGrid(ui, onPlay)
        }
    }
}

@Composable
private fun EpisodeGrid(ui: DramaUi, onPlay: (String) -> Unit) {
    // Open on the row of the episode to continue, so the user never scrolls through dozens of episodes.
    val targetId = ui.target?.episode?.id
    val targetIndex = ui.episodes.indexOfFirst { it.id == targetId }.coerceAtLeast(0)
    val state = rememberLazyGridState(initialFirstVisibleItemIndex = (targetIndex / GRID_COLUMNS) * GRID_COLUMNS)
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        state = state,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(14.dp),
    ) {
        itemsIndexed(ui.episodes, key = { _, e -> e.id }) { index, ep ->
            val req = remember { FocusRequester() }
            if (index == targetIndex) LaunchedEffect(Unit) { runCatching { req.requestFocus() } }
            EpisodeCard(
                episode = ep, entry = ui.entries[ep.id], isNext = index == targetIndex,
                onClick = { onPlay(ep.id) }, modifier = Modifier.focusRequester(req),
            )
        }
    }
}

@Composable
private fun EpisodeCard(episode: Episode, entry: WatchEntry?, isNext: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    val c = LocalDramaColors.current
    FocusCard(onClick = onClick, modifier = modifier.fillMaxWidth().height(210.dp), shape = RoundedCornerShape(14.dp)) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(placeholderBrush(episode.dramaId + episode.number))) {
                if (episode.thumbnailUrl != null) {
                    AsyncImage(episode.thumbnailUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
            Box(Modifier.fillMaxSize().background(Color(0x66000000)))
            Column(Modifier.align(Alignment.CenterStart).padding(18.dp)) {
                Text(s.episode(episode.number), style = DramaType.title, color = Color.White)
                episode.title?.let { Text(it, style = DramaType.label, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            }
            val status = when {
                entry?.watched == true -> s.watchedLabel
                entry != null && !entry.progressConfirmed -> s.openedLabel
                else -> null
            }
            Column(Modifier.align(Alignment.TopEnd).padding(10.dp), horizontalAlignment = Alignment.End) {
                if (isNext) Text(
                    "▶ " + if (entry != null && !entry.watched) s.continueEpisode(episode.number).substringBefore(" ").trim() else s.play,
                    style = DramaType.label, color = c.onAccent,
                    modifier = Modifier.background(c.accent, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
                )
                if (status != null) Text(
                    (if (entry?.watched == true) "✓ " else "") + status, style = DramaType.label, color = Color.White,
                    modifier = Modifier.padding(top = 6.dp).background(Color(0xAA000000), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            ProgressRules.fraction(entry)?.let { f ->
                ProgressBar(f, Modifier.align(Alignment.BottomStart).fillMaxWidth().height(8.dp))
            }
        }
    }
}
