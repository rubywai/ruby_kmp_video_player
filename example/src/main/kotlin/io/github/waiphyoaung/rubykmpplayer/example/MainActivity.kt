package io.github.waiphyoaung.rubykmpplayer.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.waiphyoaung.rubykmpplayer.RubyAndroidPlayerView
import io.github.waiphyoaung.rubykmpplayer.RubyAndroidVideoPlayerController
import io.github.waiphyoaung.rubykmpplayer.RubyPlayerConfig
import io.github.waiphyoaung.rubykmpplayer.RubyPlayerSnapshot
import io.github.waiphyoaung.rubykmpplayer.RubyVideoSource

class MainActivity : ComponentActivity() {
    private lateinit var controller: RubyAndroidVideoPlayerController
    private var playerView: RubyAndroidPlayerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller = RubyAndroidVideoPlayerController(this)
        setContent {
            RubyPlayerExampleScreen(controller)
        }
    }

    override fun onDestroy() {
        playerView?.unbind()
        controller.release()
        super.onDestroy()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun RubyPlayerExampleScreen(controller: RubyAndroidVideoPlayerController) {
        val snapshot by controller.snapshots.collectAsState()
        var url by remember { mutableStateOf(DEFAULT_VIDEO_URL) }

        MaterialTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text("Ruby KMP Player", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    "Android example",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    )
                },
            ) { contentPadding ->
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(contentPadding)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    Text(
                        "Play a remote video",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "A small integration example using the shared Ruby player API.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))

                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        factory = { context ->
                            RubyAndroidPlayerView(context).also {
                                playerView = it
                                it.bind(controller)
                            }
                        },
                        update = { it.bind(controller) },
                    )
                    Spacer(Modifier.height(12.dp))

                    PlayerStatus(snapshot)
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
                        onClick = {
                            controller.load(
                                RubyVideoSource(url.trim()),
                                RubyPlayerConfig(autoPlay = true),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Load video")
                    }
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        OutlinedButton(onClick = controller::play) { Text("Play") }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = controller::pause) { Text("Pause") }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = controller::stop,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        ) { Text("Stop") }
                    }
                }
            }
        }
    }

    @Composable
    private fun PlayerStatus(snapshot: RubyPlayerSnapshot) {
        val statusColor = when (snapshot.state.name) {
            "Playing" -> Color(0xFF1B7F4B)
            "Error" -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.primary
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = statusColor.copy(alpha = 0.10f),
            shape = RoundedCornerShape(12.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(snapshot.state.name, color = statusColor)
                Text(
                    "${snapshot.positionMs / 1_000}s / ${snapshot.durationMs / 1_000}s",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    private companion object {
        const val DEFAULT_VIDEO_URL =
            "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"
    }
}
