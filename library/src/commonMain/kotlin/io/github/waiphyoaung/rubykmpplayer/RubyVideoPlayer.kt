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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
public expect fun RubyNativePlayerSurface(
    controller: RubyVideoPlayerController,
    modifier: Modifier,
)

@Composable
public expect fun RubyFullscreenSystemUi()

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
    val content: @Composable (Modifier) -> Unit = { surfaceModifier ->
        Box(surfaceModifier.background(Color.Black)) {
            RubyNativePlayerSurface(controller, Modifier.fillMaxSize())
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
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                if (controls.showSeekBar) {
                    SeekBar(snapshot, controller)
                }
                ControlBar(
                    snapshot = snapshot,
                    controller = controller,
                    controls = controls,
                    onFullscreen = onFullscreen,
                    onExitFullscreen = onExitFullscreen,
                    fullscreen = fullscreen,
                )
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
private fun SeekBar(snapshot: RubyPlayerSnapshot, controller: RubyVideoPlayerController) {
    val duration = snapshot.durationMs.coerceAtLeast(1L)
    Slider(
        value = snapshot.positionMs.coerceIn(0L, duration).toFloat(),
        onValueChange = { controller.seekTo(it.toLong()) },
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
                IconButton(onClick = controller::stop) {
                    Icon(Icons.Filled.Stop, contentDescription = "Stop", tint = Color.White)
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
                IconButton(onClick = if (fullscreen) onExitFullscreen else onFullscreen) {
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
