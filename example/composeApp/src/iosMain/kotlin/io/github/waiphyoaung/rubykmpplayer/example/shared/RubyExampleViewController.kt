package io.github.waiphyoaung.rubykmpplayer.example.shared

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

public fun RubyExampleViewController(): UIViewController =
    ComposeUIViewController { RubyPlayerScreen() }
