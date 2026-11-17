package com.dramaease.tv.ui.components

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.dramaease.tv.data.ProgressRules
import com.dramaease.tv.model.Drama
import com.dramaease.tv.ui.theme.DramaType
import com.dramaease.tv.ui.theme.LocalDramaColors

/** Ignores repeated OK presses within [intervalMs] so a double press never opens a screen twice. */
@Composable
fun rememberDebounced(intervalMs: Long = 600L, onClick: () -> Unit): () -> Unit {
    var last by remember { mutableLongStateOf(0L) }
    return {
        val now = SystemClock.elapsedRealtime()
        if (now - last >= intervalMs) {
            last = now
            onClick()
        }
    }
}

/** Focusable, clickable container with a thick, always-visible focus ring (remote-control friendly). */
@Composable
fun FocusCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(14.dp),
    containerColor: Color = LocalDramaColors.current.surface,
    focusedContainerColor: Color = LocalDramaColors.current.surfaceFocused,
    content: @Composable () -> Unit,
) {
    val c = LocalDramaColors.current
    Surface(
        onClick = rememberDebounced(onClick = onClick),
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = containerColor, contentColor = c.text,
            focusedContainerColor = focusedContainerColor, focusedContentColor = c.text,
            pressedContainerColor = focusedContainerColor, pressedContentColor = c.text,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(5.dp, c.focusRing), shape = shape),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        content = { content() },
    )
}

@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
) {
    val c = LocalDramaColors.current
    FocusCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        containerColor = if (primary) c.accent else c.surface,
        focusedContainerColor = if (primary) c.accent else c.surfaceFocused,
    ) {
        Box(
            Modifier.padding(horizontal = 36.dp, vertical = if (primary) 22.dp else 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text, style = if (primary) DramaType.button else DramaType.label,
                color = if (primary) c.onAccent else c.text, textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = DramaType.section, color = LocalDramaColors.current.text, modifier = modifier)
}

/** Poster art: real image when a URL exists, otherwise a title card drawn by the app (nothing is faked). */
@Composable
fun PosterArt(drama: Drama, modifier: Modifier = Modifier) {
    val c = LocalDramaColors.current
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(placeholderBrush(drama.title)), contentAlignment = Alignment.Center) {
        if (drama.posterUrl != null) {
            AsyncImage(
                model = drama.posterUrl, contentDescription = drama.title,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                drama.title, style = DramaType.title, color = Color.White, textAlign = TextAlign.Center,
                maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(16.dp),
            )
        }
    }
}

private val placeholderPalette = listOf(
    Color(0xFF5B3A8C) to Color(0xFF1F1136), Color(0xFF8C3A4F) to Color(0xFF36111C),
    Color(0xFF2F6F8F) to Color(0xFF0E2A38), Color(0xFF3F7F4F) to Color(0xFF10301A),
    Color(0xFF8F6A2F) to Color(0xFF38280E), Color(0xFF5A5F8F) to Color(0xFF171A38),
)

fun placeholderBrush(seed: String): Brush {
    val (a, b) = placeholderPalette[(seed.hashCode() and 0x7fffffff) % placeholderPalette.size]
    return Brush.verticalGradient(listOf(a, b))
}

@Composable
fun PosterCard(
    drama: Drama,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
) {
    val c = LocalDramaColors.current
    Column(modifier.width(210.dp)) {
        FocusCard(onClick = onClick, modifier = Modifier.size(width = 210.dp, height = 300.dp)) {
            Box(Modifier.fillMaxSize()) {
                PosterArt(drama, Modifier.fillMaxSize())
                if (badge != null) {
                    Text(
                        badge, style = DramaType.label, color = c.onAccent,
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                            .background(c.accent, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
        Text(
            drama.title, style = DramaType.label, color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 14.dp, start = 4.dp, end = 4.dp),
        )
    }
}

/** Wide Continue Watching card: poster art, episode label and a progress bar ONLY when progress is confirmed. */
@Composable
fun ContinueCard(
    drama: Drama,
    episodeLabel: String,
    subLabel: String?,
    fraction: Float?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalDramaColors.current
    Column(modifier.width(360.dp)) {
        FocusCard(onClick = onClick, modifier = Modifier.size(width = 360.dp, height = 203.dp)) {
            Box(Modifier.fillMaxSize()) {
                PosterArt(drama, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))))
                Column(Modifier.align(Alignment.BottomStart).padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(episodeLabel, style = DramaType.title, color = Color.White)
                    if (subLabel != null) Text(subLabel, style = DramaType.label, color = Color.White)
                }
                if (fraction != null) ProgressBar(fraction, Modifier.align(Alignment.BottomStart).fillMaxWidth().height(8.dp))
            }
        }
        Text(
            drama.title, style = DramaType.label, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 12.dp, start = 4.dp),
        )
    }
}

@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    val c = LocalDramaColors.current
    Box(modifier.background(Color(0x66FFFFFF))) {
        Box(Modifier.fillMaxSize().fillFraction(fraction).background(c.progress))
    }
}

private fun Modifier.fillFraction(f: Float) = this.then(Modifier.fillMaxWidth(f.coerceIn(0f, 1f)))

@Composable
fun MessagePanel(text: String, modifier: Modifier = Modifier) {
    Text(
        text, style = DramaType.body, color = LocalDramaColors.current.textDim,
        modifier = modifier.padding(vertical = 8.dp),
    )
}

@Suppress("unused")
private val keepProgressRules = ProgressRules.WATCHED_FRACTION
