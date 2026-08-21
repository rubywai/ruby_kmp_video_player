package io.github.waiphyoaung.rubykmpplayer.example.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.waiphyoaung.rubykmpplayer.RubyPlayerConfig
import io.github.waiphyoaung.rubykmpplayer.RubyPlayerControls
import io.github.waiphyoaung.rubykmpplayer.RubyVideoContentType
import io.github.waiphyoaung.rubykmpplayer.RubyVideoPlayer
import io.github.waiphyoaung.rubykmpplayer.RubyVideoPlayerController
import io.github.waiphyoaung.rubykmpplayer.RubyVideoSource

private enum class ExampleScreen {
    Home,
    Mp4,
    Hls,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun RubyPlayerScreen(controller: RubyVideoPlayerController) {
    var screen by remember { mutableStateOf(ExampleScreen.Home) }

    when (screen) {
        ExampleScreen.Home -> RubyPlayerHomeScreen(
            onOpenVideo = { screen = ExampleScreen.Mp4 },
            onOpenHls = { screen = ExampleScreen.Hls },
        )
        ExampleScreen.Mp4 -> RubyVideoExampleScreen(
            controller = controller,
            onBack = { screen = ExampleScreen.Home },
        )
        ExampleScreen.Hls -> RubyHlsExampleScreen(
            controller = controller,
            onBack = { screen = ExampleScreen.Home },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RubyPlayerHomeScreen(
    onOpenVideo: () -> Unit,
    onOpenHls: () -> Unit,
) {
    MaterialTheme {
        Scaffold(
            topBar = { RubyTopBar(title = "Ruby KMP Player") },
        ) { padding ->
            Column(
                modifier = Modifier.padding(padding).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Choose a playback example", style = MaterialTheme.typography.titleMedium)
                Button(onClick = onOpenVideo, modifier = Modifier.fillMaxWidth()) {
                    Text("1. MP4")
                }
                Button(onClick = onOpenHls, modifier = Modifier.fillMaxWidth()) {
                    Text("2. HLS")
                }
            }
        }
    }
}

@Composable
private fun RubyVideoExampleScreen(
    controller: RubyVideoPlayerController,
    onBack: () -> Unit,
) {
    RubyPlayerDemoScreen(
        controller = controller,
        onBack = onBack,
        title = "MP4 Player",
        label = "Progressive video",
        initialUrl = DEFAULT_VIDEO_URL,
        contentType = RubyVideoContentType.Progressive,
    )
}

@Composable
private fun RubyHlsExampleScreen(
    controller: RubyVideoPlayerController,
    onBack: () -> Unit,
) {
    RubyPlayerDemoScreen(
        controller = controller,
        onBack = onBack,
        title = "HLS Player",
        label = "HTTP Live Streaming",
        initialUrl = DEFAULT_HLS_URL,
        contentType = RubyVideoContentType.Hls,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RubyPlayerDemoScreen(
    controller: RubyVideoPlayerController,
    onBack: () -> Unit,
    title: String,
    label: String,
    initialUrl: String,
    contentType: RubyVideoContentType,
) {
    LaunchedEffect(initialUrl, contentType) {
        controller.load(
            RubyVideoSource(initialUrl, contentType = contentType),
            RubyPlayerConfig(autoPlay = true),
        )
    }
    DisposableEffect(Unit) {
        onDispose { controller.stop() }
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                RubyTopBar(title = title, onBack = onBack)
            },
        ) { padding ->
            Column(Modifier.padding(padding).padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                RubyVideoPlayer(
                    controller = controller,
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    controls = RubyPlayerControls(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RubyTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(),
    )
}

private const val DEFAULT_VIDEO_URL =
    "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"
private const val DEFAULT_HLS_URL =
    "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
