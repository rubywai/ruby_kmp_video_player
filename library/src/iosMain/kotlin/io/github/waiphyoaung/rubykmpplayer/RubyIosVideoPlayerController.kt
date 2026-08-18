package io.github.waiphyoaung.rubykmpplayer

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.AVFoundation.AVPlayer
import platform.AVKit.AVPlayerViewController
import platform.Foundation.NSURL
import io.github.waiphyoaung.rubykmpplayer.bridge.ruby_av_player_load
import io.github.waiphyoaung.rubykmpplayer.bridge.ruby_av_player_pause
import io.github.waiphyoaung.rubykmpplayer.bridge.ruby_av_player_play
import io.github.waiphyoaung.rubykmpplayer.bridge.ruby_av_player_seek
import io.github.waiphyoaung.rubykmpplayer.bridge.ruby_av_player_stop

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

        ruby_av_player_load(avPlayer, url)
        mutableSnapshots.value = snapshot(RubyPlaybackState.Ready)
        if (config.autoPlay) play()
    }

    override fun play() {
        ruby_av_player_play(avPlayer)
        mutableSnapshots.value = snapshot(RubyPlaybackState.Playing)
    }

    override fun pause() {
        ruby_av_player_pause(avPlayer)
        mutableSnapshots.value = snapshot(RubyPlaybackState.Paused)
    }

    override fun stop() {
        ruby_av_player_stop(avPlayer)
        mutableSnapshots.value = RubyPlayerSnapshot()
    }

    override fun seekTo(positionMs: Long) {
        ruby_av_player_seek(avPlayer, positionMs / 1_000.0)
        mutableSnapshots.value = snapshot(mutableSnapshots.value.state, positionMs = positionMs)
    }

    override fun release() {
        stop()
    }

    public fun createPlayerViewController(): AVPlayerViewController =
        AVPlayerViewController().also {
            it.player = avPlayer
            it.showsPlaybackControls = false
        }

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
