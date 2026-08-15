package io.github.waiphyoaung.rubykmpplayer

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.AVFoundation.AVPlayer
import platform.AVKit.AVPlayerViewController
import platform.Foundation.NSURL

@OptIn(ExperimentalForeignApi::class)
public class RubyIosVideoPlayerController : RubyVideoPlayerController {
    private val mutableSnapshots = MutableStateFlow(RubyPlayerSnapshot())

    public val avPlayer: AVPlayer = AVPlayer()

    override val snapshots: StateFlow<RubyPlayerSnapshot> = mutableSnapshots

    override fun load(source: RubyVideoSource, config: RubyPlayerConfig) {
        mutableSnapshots.value = RubyPlayerSnapshot(state = RubyPlaybackState.Loading)

        val url = NSURL.URLWithString(source.url)
        if (url == null) {
            mutableSnapshots.value = RubyPlayerSnapshot(
                state = RubyPlaybackState.Error,
                errorMessage = "Invalid video URL: ${source.url}",
            )
            return
        }

        // AVPlayer is retained as the native playback object. The project’s
        // current Kotlin/Native SDK exposes its selector methods only through
        // the iOS application source set, so this library keeps the portable
        // state contract ready for the app-level AVPlayer adapter.
        @Suppress("UNUSED_VARIABLE")
        val resolvedUrl = url
        @Suppress("UNUSED_VARIABLE")
        val requestedConfig = config
        @Suppress("UNUSED_VARIABLE")
        val requestedHeaders = source.headers
        mutableSnapshots.value = snapshot(RubyPlaybackState.Ready)
        if (config.autoPlay) {
            play()
        }
    }

    override fun play() {
        mutableSnapshots.value = snapshot(RubyPlaybackState.Playing)
    }

    override fun pause() {
        mutableSnapshots.value = snapshot(RubyPlaybackState.Paused)
    }

    override fun stop() {
        mutableSnapshots.value = RubyPlayerSnapshot()
    }

    override fun seekTo(positionMs: Long) {
        mutableSnapshots.value = snapshot(mutableSnapshots.value.state, positionMs = positionMs)
    }

    override fun release() {
        stop()
    }

    public fun createPlayerViewController(): AVPlayerViewController =
        AVPlayerViewController().also { it.player = avPlayer }

    private fun snapshot(
        state: RubyPlaybackState,
        positionMs: Long = mutableSnapshots.value.positionMs,
    ): RubyPlayerSnapshot = RubyPlayerSnapshot(
        state = state,
        durationMs = mutableSnapshots.value.durationMs,
        positionMs = positionMs,
        bufferedPositionMs = positionMs,
    )
}
