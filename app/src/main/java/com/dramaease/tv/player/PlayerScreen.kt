package com.dramaease.tv.player

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import com.dramaease.tv.data.AppSettings
import com.dramaease.tv.data.TextSize
import com.dramaease.tv.ui.PlayerUi
import com.dramaease.tv.ui.PlayerViewModel
import com.dramaease.tv.ui.components.BigButton
import com.dramaease.tv.ui.components.FocusCard
import com.dramaease.tv.ui.theme.DramaType
import com.dramaease.tv.ui.theme.LocalDramaColors
import com.dramaease.tv.ui.theme.LocalStrings
import kotlinx.coroutines.delay

private const val SEEK_STEP_MS = 10_000L
private const val SAVE_EVERY_TICKS = 5
private const val CONTROLS_HIDE_MS = 6_000L

@Composable
fun PlayerScreen(
    vm: PlayerViewModel,
    settings: AppSettings,
    onBack: () -> Unit,
    onPlayEpisode: (episodeId: String) -> Unit,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val c = LocalDramaColors.current
    BackHandler { onBack() }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when (val state = ui) {
            PlayerUi.Loading -> CenterMessage(LocalStrings.current.loading)
            PlayerUi.Unavailable -> ErrorPanel(LocalStrings.current.unavailable, onRetry = vm::load, onBack = onBack)
            is PlayerUi.External -> ExternalPanel(state, onOpened = vm::markOpenedExternally, onBack = onBack, onPlayEpisode = onPlayEpisode)
            is PlayerUi.Native -> NativePlayer(state, vm, settings, onBack, onPlayEpisode)
        }
    }
    @Suppress("UNUSED_EXPRESSION") c
}

@Composable
private fun CenterMessage(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = DramaType.title, color = Color.White)
    }
}

@Composable
private fun ErrorPanel(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    val s = LocalStrings.current
    val retry = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { retry.requestFocus() } }
    Column(
        Modifier.fillMaxSize().padding(64.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, style = DramaType.title, color = Color.White, textAlign = TextAlign.Center)
        BigButton(s.tryAgain, onClick = onRetry, primary = true, modifier = Modifier.focusRequester(retry))
        BigButton(s.goBack, onClick = onBack)
    }
}

/** Playback that only exists on the original website. We say so plainly and never record it as watched. */
@Composable
private fun ExternalPanel(state: PlayerUi.External, onOpened: () -> Unit, onBack: () -> Unit, onPlayEpisode: (String) -> Unit) {
    val s = LocalStrings.current
    val context = LocalContext.current
    var noBrowser by remember { mutableStateOf(false) }
    var showWeb by remember { mutableStateOf(false) }
    if (showWeb) {
        WebsiteScreen(
            url = state.option.pageUrl, onBack = onBack,
            onNext = state.next?.let { n -> { onPlayEpisode(n.id) } },
        )
        return
    }
    val openReq = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { openReq.requestFocus() } }
    Column(
        Modifier.fillMaxSize().padding(64.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("${state.drama.title} — ${s.episode(state.episode.number)}", style = DramaType.headline, color = Color.White, textAlign = TextAlign.Center)
        Text(s.externalExplain, style = DramaType.body, color = Color(0xFFDDDDDD), textAlign = TextAlign.Center)
        if (noBrowser) Text(s.noBrowser, style = DramaType.body, color = LocalDramaColors.current.error, textAlign = TextAlign.Center)
        BigButton(
            s.watchHere, primary = true, modifier = Modifier.focusRequester(openReq),
            onClick = { onOpened(); showWeb = true }, // only "opened" is recorded; viewing cannot be observed
        )
        BigButton(
            s.openInBrowser,
            onClick = {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(state.option.pageUrl)))
                    onOpened()
                } catch (_: ActivityNotFoundException) {
                    noBrowser = true
                }
            },
        )
        BigButton(s.goBack, onClick = onBack)
    }
}

@Composable
private fun NativePlayer(
    state: PlayerUi.Native,
    vm: PlayerViewModel,
    settings: AppSettings,
    onBack: () -> Unit,
    onPlayEpisode: (String) -> Unit,
) {
    val s = LocalStrings.current
    val c = LocalDramaColors.current
    val context = LocalContext.current
    val view = LocalView.current

    var isPlaying by remember { mutableStateOf(false) }
    var buffering by remember { mutableStateOf(true) }
    var ended by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var controlsVisible by remember { mutableStateOf(true) }
    var interaction by remember { mutableLongStateOf(0L) }
    var subtitleLabel by remember { mutableStateOf<String?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }

    val player = remember(state.episode.id) { ExoPlayer.Builder(context).build() }
    val latest by rememberUpdatedState(state)

    fun save(endedNow: Boolean = false) {
        val dur = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: durationMs
        val pos = player.currentPosition
        // Do not record before playback truly started: an early exit must not overwrite good progress with 0.
        if (pos > 0 || endedNow) vm.saveProgress(pos, dur, endedNow)
    }

    fun updateSubtitleLabel() {
        val groups = player.currentTracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
        val disabled = C.TRACK_TYPE_TEXT in player.trackSelectionParameters.disabledTrackTypes
        subtitleLabel = when {
            groups.isEmpty() -> null
            disabled || groups.none { it.isSelected } -> s.subtitlesOff
            else -> groups.first { it.isSelected }.mediaTrackGroup.getFormat(0).label ?: s.subtitlesOn
        }
    }

    fun cycleSubtitles() {
        val groups = player.currentTracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
        if (groups.isEmpty()) return
        val disabled = C.TRACK_TYPE_TEXT in player.trackSelectionParameters.disabledTrackTypes
        val current = if (disabled) -1 else groups.indexOfFirst { it.isSelected }
        val next = current + 1
        val b = player.trackSelectionParameters.buildUpon()
        if (next >= groups.size) {
            b.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        } else {
            b.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            b.setOverrideForType(TrackSelectionOverride(groups[next].mediaTrackGroup, 0))
        }
        player.trackSelectionParameters = b.build()
    }

    fun seekBy(deltaMs: Long) {
        val dur = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: Long.MAX_VALUE
        player.seekTo((player.currentPosition + deltaMs).coerceIn(0L, dur))
        positionMs = player.currentPosition
        toast = if (deltaMs > 0) s.forward10 else s.back10
    }

    fun togglePlay() { if (player.isPlaying) player.pause() else { if (ended) player.seekTo(0); player.play() } }

    DisposableEffect(player) {
        view.keepScreenOn = true
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_ENDED && !ended) {
                    ended = true
                    controlsVisible = true
                    save(endedNow = true)
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                // Details go to Logcat only; the viewer sees a friendly message.
                android.util.Log.w("DramaEase", "Playback error code=${error.errorCodeName}")
                failed = true
            }
            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) { updateSubtitleLabel() }
        }
        player.addListener(listener)
        val sub = latest.option.subtitles.map {
            MediaItem.SubtitleConfiguration.Builder(Uri.parse(it.url))
                .setMimeType(it.mimeType).setLanguage(it.language).setLabel(it.label)
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT).build()
        }
        player.setMediaItem(
            MediaItem.Builder().setUri(latest.option.url).setSubtitleConfigurations(sub).build(),
            latest.resumeMs,
        )
        player.prepare()
        player.playWhenReady = true
        onDispose {
            view.keepScreenOn = false
            save() // keep the latest position before the player goes away
            player.removeListener(listener)
            player.release()
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { player.pause(); save() }

    // Progress ticker: UI position every second, database save every few seconds. No work while paused/buffering.
    LaunchedEffect(player) {
        var tick = 0
        while (true) {
            delay(1_000)
            positionMs = player.currentPosition
            durationMs = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: durationMs
            if (player.isPlaying && ++tick % SAVE_EVERY_TICKS == 0) save()
        }
    }

    // Controls auto-hide after a few idle seconds while playing.
    LaunchedEffect(controlsVisible, interaction, isPlaying, ended) {
        if (controlsVisible && isPlaying && !ended) { delay(CONTROLS_HIDE_MS); controlsVisible = false }
    }
    LaunchedEffect(toast) { if (toast != null) { delay(1_200); toast = null } }

    val rootReq = remember { FocusRequester() }
    val playReq = remember { FocusRequester() }
    val endReq = remember { FocusRequester() }
    LaunchedEffect(controlsVisible, ended) {
        runCatching { if (ended) endReq.requestFocus() else if (controlsVisible) playReq.requestFocus() else rootReq.requestFocus() }
    }

    if (failed) {
        ErrorPanel(s.playFailed, onRetry = { failed = false; player.prepare(); player.play() }, onBack = onBack)
        return
    }

    Box(
        Modifier.fillMaxSize()
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                interaction = System.nanoTime()
                when (e.key) {
                    Key.MediaPlayPause -> { togglePlay(); controlsVisible = true; true }
                    Key.MediaPlay -> { player.play(); true }
                    Key.MediaPause -> { player.pause(); controlsVisible = true; true }
                    Key.MediaFastForward -> { seekBy(SEEK_STEP_MS); true }
                    Key.MediaRewind -> { seekBy(-SEEK_STEP_MS); true }
                    Key.MediaNext -> { state.next?.let { save(); onPlayEpisode(it.id) }; true }
                    Key.MediaPrevious -> { state.previous?.let { save(); onPlayEpisode(it.id) }; true }
                    else -> if (!controlsVisible && !ended) when (e.key) {
                        // Controls hidden: Left/Right seek, anything else just brings the controls back.
                        Key.DirectionLeft -> { seekBy(-SEEK_STEP_MS); true }
                        Key.DirectionRight -> { seekBy(SEEK_STEP_MS); true }
                        Key.DirectionCenter, Key.Enter, Key.DirectionUp, Key.DirectionDown -> { controlsVisible = true; true }
                        else -> false
                    } else false
                }
            }
            .focusRequester(rootReq)
            .focusable(),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    useController = false
                    isFocusable = false
                    this.player = player
                    subtitleView?.apply {
                        setApplyEmbeddedStyles(false)
                        setApplyEmbeddedFontSizes(false)
                        // Readable background behind captions.
                        setStyle(CaptionStyleCompat(android.graphics.Color.WHITE, 0xCC000000.toInt(), android.graphics.Color.TRANSPARENT, CaptionStyleCompat.EDGE_TYPE_NONE, android.graphics.Color.WHITE, null))
                    }
                }
            },
            update = { pv -> pv.subtitleView?.setFractionalTextSize(subtitleFraction(settings.subtitleSize)) },
        )

        if (buffering && !failed) {
            AndroidView(factory = { ProgressBar(it) }, modifier = Modifier.align(Alignment.Center))
        }
        toast?.let {
            Text(
                it, style = DramaType.title, color = Color.White,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp)
                    .background(Color(0xCC000000), androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }

        if (ended) {
            Column(
                Modifier.fillMaxSize().background(Color(0xDD000000)).padding(64.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("${state.drama.title} — ${s.episode(state.episode.number)}", style = DramaType.headline, color = Color.White, textAlign = TextAlign.Center)
                if (state.next != null) {
                    BigButton(s.nextEpisode, primary = true, modifier = Modifier.focusRequester(endReq), onClick = { onPlayEpisode(state.next.id) })
                    BigButton(s.backToEpisodes, onClick = onBack)
                } else {
                    BigButton(s.backToEpisodes, primary = true, modifier = Modifier.focusRequester(endReq), onClick = onBack)
                }
            }
        } else if (controlsVisible) {
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color(0xEE000000))))
                    .padding(horizontal = 64.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text("${state.drama.title} — ${s.episode(state.episode.number)}", style = DramaType.title, color = Color.White)
                SeekBar(positionMs, durationMs, onSeek = { seekBy(it) })
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!settings.easyMode) {
                        ControlButton(s.previousEpisode, enabled = state.previous != null) { state.previous?.let { save(); onPlayEpisode(it.id) } }
                    }
                    ControlButton("⟲ 10", s.back10) { seekBy(-SEEK_STEP_MS) }
                    ControlButton(if (isPlaying) "❚❚ ${s.pause}" else "▶ ${s.play}", primary = true, modifier = Modifier.focusRequester(playReq)) { togglePlay() }
                    ControlButton("10 ⟳", s.forward10) { seekBy(SEEK_STEP_MS) }
                    if (state.next != null) ControlButton(s.nextEpisode) { save(); onPlayEpisode(state.next.id) }
                    val sl = subtitleLabel
                    if (sl != null) ControlButton("${s.subtitles}: $sl") { cycleSubtitles() }
                    else Text(s.noSubtitles, style = DramaType.label, color = Color(0xFFBBBBBB))
                    ControlButton(s.backToEpisodes) { onBack() }
                }
            }
        }
    }
    @Suppress("UNUSED_EXPRESSION") c
}

private fun subtitleFraction(size: TextSize) = when (size) {
    TextSize.NORMAL -> 0.0533f
    TextSize.LARGE -> 0.075f
    TextSize.EXTRA_LARGE -> 0.10f
}

@Composable
private fun ControlButton(
    label: String,
    contentLabel: String? = null,
    primary: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = LocalDramaColors.current
    if (!enabled) return
    FocusCard(
        onClick = onClick, modifier = modifier,
        containerColor = if (primary) c.accent else c.surface,
        focusedContainerColor = if (primary) c.accent else c.surfaceFocused,
    ) {
        Text(
            contentLabel?.let { "$label  $it" } ?: label, style = DramaType.label,
            color = if (primary) c.onAccent else c.text,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
        )
    }
}

/** Focusable progress bar: Left/Right while focused seeks by 10 seconds. */
@Composable
private fun SeekBar(positionMs: Long, durationMs: Long, onSeek: (Long) -> Unit) {
    val c = LocalDramaColors.current
    var focused by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(formatTime(positionMs), style = DramaType.label, color = Color.White)
        Box(
            Modifier.weight(1f).height(if (focused) 18.dp else 12.dp)
                .background(Color(0x66FFFFFF), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .onFocusChanged { focused = it.isFocused }
                .onPreviewKeyEvent { e ->
                    if (e.type == KeyEventType.KeyDown && focused) when (e.key) {
                        Key.DirectionLeft -> { onSeek(-SEEK_STEP_MS); true }
                        Key.DirectionRight -> { onSeek(SEEK_STEP_MS); true }
                        else -> false
                    } else false
                }
                .focusable(),
        ) {
            if (durationMs > 0) {
                Box(
                    Modifier.fillMaxSize().fillFraction((positionMs.toFloat() / durationMs).coerceIn(0f, 1f))
                        .background(c.progress, androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                )
            }
        }
        Text(if (durationMs > 0) formatTime(durationMs) else "--:--", style = DramaType.label, color = Color.White)
    }
}

private fun Modifier.fillFraction(f: Float) = this.then(Modifier.fillMaxWidth(f))

fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
