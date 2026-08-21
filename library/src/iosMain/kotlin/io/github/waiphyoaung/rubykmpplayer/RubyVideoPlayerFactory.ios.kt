package io.github.waiphyoaung.rubykmpplayer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal actual fun rememberRubyVideoPlayerController(): RubyVideoPlayerController =
    remember { RubyIosVideoPlayerController() }
