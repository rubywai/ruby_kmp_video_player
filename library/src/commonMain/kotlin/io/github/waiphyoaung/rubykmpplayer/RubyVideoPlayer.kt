package io.github.waiphyoaung.rubykmpplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
internal expect fun RubyNativePlayerSurface(
    controller: RubyVideoPlayerController,
    modifier: Modifier,
)

@Composable
internal expect fun RubyFullscreenSystemUi()

@Composable
internal expect fun rememberRubyVideoPlayerController(): RubyVideoPlayerController

/**
 * Plays [url] with a controller owned by this composable.
 *
 * The appropriate Android or iOS controller is created, loaded, observed, and
 * released by the library. Use the controller overload when direct playback
 * control or custom source headers are required.
 */
@Composable
public fun RubyVideoPlayer(
    url: String,
    modifier: Modifier = Modifier,
    config: RubyPlayerConfig = RubyPlayerConfig(),
    controls: RubyPlayerControls = RubyPlayerControls(),
) {
    val controller = rememberRubyVideoPlayerController()

    LaunchedEffect(controller, url, config) {
        controller.load(RubyVideoSource(url), config)
    }
    DisposableEffect(controller) {
        onDispose(controller::release)
    }

    RubyVideoPlayer(
        controller = controller,
        modifier = modifier,
        controls = controls,
    )
}

/** Displays the shared player UI for a caller-managed platform controller. */
@Composable
public fun RubyVideoPlayer(
    controller: RubyVideoPlayerController,
    modifier: Modifier = Modifier,
    controls: RubyPlayerControls = RubyPlayerControls(),
) {
    val snapshot by controller.snapshots.collectAsState()
    var fullscreen by remember { mutableStateOf(false) }

    PlayerFrame(
        controller = controller,
        snapshot = snapshot,
        modifier = modifier,
        controls = controls,
        onFullscreen = { fullscreen = true },
        onExitFullscreen = { fullscreen = false },
        fullscreen = fullscreen,
    )
}

@Composable
private fun PlayerFrame(
    controller: RubyVideoPlayerController,
    snapshot: RubyPlayerSnapshot,
    modifier: Modifier,
    controls: RubyPlayerControls,
    onFullscreen: () -> Unit,
    onExitFullscreen: () -> Unit,
    fullscreen: Boolean,
) {
    var controlsVisible by remember { mutableStateOf(true) }
    var interactionVersion by remember { mutableStateOf(0) }

    LaunchedEffect(
        snapshot.state,
        controls.autoHide,
        controls.autoHideDelayMillis,
        interactionVersion,
    ) {
        if (snapshot.state != RubyPlaybackState.Playing || !controls.autoHide) {
            controlsVisible = true
        } else {
            kotlinx.coroutines.delay(controls.autoHideDelayMillis.coerceAtLeast(0L))
            controlsVisible = false
        }
    }

    fun registerInteraction() {
        controlsVisible = true
        interactionVersion++
    }

    val content: @Composable (Modifier) -> Unit = { surfaceModifier ->
        Box(surfaceModifier.background(Color.Black)) {
            RubyNativePlayerSurface(controller, Modifier.fillMaxSize())
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(snapshot.state, controls.autoHide) {
                        detectTapGestures {
                            if (snapshot.state == RubyPlaybackState.Playing) {
                                controlsVisible = !controlsVisible
                            } else {
                                controlsVisible = true
                            }
                            interactionVersion++
                        }
                    },
            )
            if (snapshot.state == RubyPlaybackState.Loading || snapshot.state == RubyPlaybackState.Buffering) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
            if (snapshot.state == RubyPlaybackState.Error) {
                Text(
                    text = snapshot.errorMessage ?: "Unable to play video",
                    modifier = Modifier.align(Alignment.Center).padding(20.dp),
                    color = Color.White,
                )
            }
            if (controlsVisible || snapshot.state != RubyPlaybackState.Playing) {
                Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                    if (controls.showSeekBar) {
                        SeekBar(snapshot, controller, ::registerInteraction)
                    }
                    ControlBar(
                        snapshot = snapshot,
                        controller = controller,
                        controls = controls,
                        onFullscreen = onFullscreen,
                        onExitFullscreen = onExitFullscreen,
                        fullscreen = fullscreen,
                        onInteraction = ::registerInteraction,
                    )
                }
            }
        }
    }

    if (fullscreen) {
        Dialog(
            onDismissRequest = onExitFullscreen,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            RubyFullscreenSystemUi()
            content(Modifier.fillMaxSize())
        }
    } else {
        content(modifier)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeekBar(
    snapshot: RubyPlayerSnapshot,
    controller: RubyVideoPlayerController,
    onInteraction: () -> Unit,
) {
    val duration = snapshot.durationMs.coerceAtLeast(1L)
    Slider(
        value = snapshot.positionMs.coerceIn(0L, duration).toFloat(),
        onValueChange = {
            onInteraction()
            controller.seekTo(it.toLong())
        },
        valueRange = 0f..duration.toFloat(),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp).height(20.dp),
        thumb = {
            SliderDefaults.Thumb(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                thumbSize = androidx.compose.ui.unit.DpSize(10.dp, 10.dp),
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                modifier = Modifier.height(3.dp),
                thumbTrackGapSize = 0.dp,
            )
        },
    )
}

@Composable
private fun ControlBar(
    snapshot: RubyPlayerSnapshot,
    controller: RubyVideoPlayerController,
    controls: RubyPlayerControls,
    onFullscreen: () -> Unit,
    onExitFullscreen: () -> Unit,
    fullscreen: Boolean,
    onInteraction: () -> Unit,
) {
    Surface(color = Color.Black.copy(alpha = 0.58f)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (controls.showPlayPause) {
                IconButton(
                    onClick = {
                        onInteraction()
                        if (snapshot.state == RubyPlaybackState.Playing) controller.pause() else controller.play()
                    },
                ) {
                    Icon(
                        imageVector = if (snapshot.state == RubyPlaybackState.Playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (snapshot.state == RubyPlaybackState.Playing) "Pause" else "Play",
                        tint = Color.White,
                    )
                }
            }
            if (controls.showStop) {
                IconButton(onClick = {
                    onInteraction()
                    controller.stop()
                }) {
                    Icon(Icons.Filled.Stop, contentDescription = "Stop", tint = Color.White)
                }
            }
            if (controls.showMute) {
                IconButton(onClick = {
                    onInteraction()
                    controller.setMuted(!snapshot.muted)
                }) {
                    Icon(
                        imageVector = if (snapshot.muted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                        contentDescription = if (snapshot.muted) "Unmute" else "Mute",
                        tint = Color.White,
                    )
                }
            }
            if (controls.showStatus) {
                Text(
                    text = "${formatTime(snapshot.positionMs)} / ${formatTime(snapshot.durationMs)}",
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
            }
            if (controls.showFullscreen) {
                IconButton(onClick = {
                    onInteraction()
                    if (fullscreen) onExitFullscreen() else onFullscreen()
                }) {
                    Icon(
                        imageVector = if (fullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                        contentDescription = if (fullscreen) "Exit fullscreen" else "Fullscreen",
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
