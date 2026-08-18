package io.github.waiphyoaung.rubykmpplayer.example.shared

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.waiphyoaung.rubykmpplayer.RubyPlayerConfig
import io.github.waiphyoaung.rubykmpplayer.RubyPlayerControls
import io.github.waiphyoaung.rubykmpplayer.RubyVideoPlayer
import io.github.waiphyoaung.rubykmpplayer.RubyVideoPlayerController
import io.github.waiphyoaung.rubykmpplayer.RubyVideoSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun RubyPlayerScreen(controller: RubyVideoPlayerController) {
    var url by remember { mutableStateOf(DEFAULT_VIDEO_URL) }

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Ruby KMP Player", style = MaterialTheme.typography.titleLarge)
                            Text("Compose Multiplatform example", style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(),
                )
            },
        ) { padding ->
            Column(Modifier.padding(padding).padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text("Library-provided player controls", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                RubyVideoPlayer(
                    controller = controller,
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    controls = RubyPlayerControls(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Video URL") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { controller.load(RubyVideoSource(url.trim()), RubyPlayerConfig(autoPlay = true)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Load video") }
            }
        }
    }
}

private const val DEFAULT_VIDEO_URL =
    "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"
