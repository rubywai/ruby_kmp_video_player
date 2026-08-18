package io.github.waiphyoaung.rubykmpplayer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitViewController
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
@Composable
public actual fun RubyNativePlayerSurface(
    controller: RubyVideoPlayerController,
    modifier: Modifier,
) {
    val iosController = controller as RubyIosVideoPlayerController
    UIKitViewController(
        modifier = modifier,
        factory = { iosController.createPlayerViewController() },
    )
}
