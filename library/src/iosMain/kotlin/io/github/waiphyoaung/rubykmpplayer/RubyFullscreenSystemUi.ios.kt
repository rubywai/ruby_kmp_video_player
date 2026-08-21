package io.github.waiphyoaung.rubykmpplayer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import io.github.waiphyoaung.rubykmpplayer.bridge.ruby_request_landscape_orientation
import io.github.waiphyoaung.rubykmpplayer.bridge.ruby_request_portrait_orientation
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
@Composable
public actual fun RubyFullscreenSystemUi() {
    DisposableEffect(Unit) {
        ruby_request_landscape_orientation()
        onDispose { ruby_request_portrait_orientation() }
    }
}
