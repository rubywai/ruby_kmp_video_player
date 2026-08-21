package io.github.waiphyoaung.rubykmpplayer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberRubyVideoPlayerController(): RubyVideoPlayerController {
    val context = LocalContext.current.applicationContext
    return remember(context) { RubyAndroidVideoPlayerController(context) }
}
