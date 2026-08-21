package io.github.waiphyoaung.rubykmpplayer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
internal actual fun RubyNativePlayerSurface(
    controller: RubyVideoPlayerController,
    modifier: Modifier,
) {
    val androidController = controller as RubyAndroidVideoPlayerController
    AndroidView(
        modifier = modifier,
        factory = { context -> RubyAndroidPlayerView(context).also { it.bind(androidController) } },
        update = { it.bind(androidController) },
    )
}
