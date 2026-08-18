package io.github.waiphyoaung.rubykmpplayer.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.waiphyoaung.rubykmpplayer.RubyAndroidVideoPlayerController
import io.github.waiphyoaung.rubykmpplayer.example.shared.RubyPlayerScreen

class MainActivity : ComponentActivity() {
    private lateinit var controller: RubyAndroidVideoPlayerController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller = RubyAndroidVideoPlayerController(this)
        setContent {
            RubyPlayerScreen(controller)
        }
    }

    override fun onDestroy() {
        controller.release()
        super.onDestroy()
    }
}
