package io.github.waiphyoaung.rubykmpplayer.example.shared

import androidx.compose.ui.window.ComposeUIViewController
import io.github.waiphyoaung.rubykmpplayer.RubyIosVideoPlayerController
import platform.UIKit.UIViewController

public fun RubyExampleViewController(): UIViewController {
    val controller = RubyIosVideoPlayerController()
    return ComposeUIViewController { RubyPlayerScreen(controller) }
}
