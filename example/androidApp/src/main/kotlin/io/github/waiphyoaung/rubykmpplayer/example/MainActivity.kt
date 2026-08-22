package io.github.waiphyoaung.rubykmpplayer.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.waiphyoaung.rubykmpplayer.example.shared.RubyPlayerScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RubyPlayerScreen()
        }
    }
}
