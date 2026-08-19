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
import io.github.waiphyoaung.rubykmpplayer.bridge.ruby_av_player_set_rate
import io.github.waiphyoaung.rubykmpplayer.bridge.ruby_av_player_set_looping
import io.github.waiphyoaung.rubykmpplayer.bridge.ruby_av_player_set_volume
import io.github.waiphyoaung.rubykmpplayer.bridge.ruby_av_player_stop

@OptIn(ExperimentalForeignApi::class)
public class RubyIosVideoPlayerController : RubyVideoPlayerController {
    private val mutableSnapshots = MutableStateFlow(RubyPlayerSnapshot())
    private var volume = 1f
    private var playbackSpeed = 1f
    private var looping = false

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
        volume = config.volume
        playbackSpeed = config.playbackSpeed
        looping = config.looping
        ruby_av_player_set_volume(avPlayer, config.volume)
        ruby_av_player_set_rate(avPlayer, playbackSpeed)
        ruby_av_player_set_looping(avPlayer, looping)
        mutableSnapshots.value = snapshot(RubyPlaybackState.Ready)
        if (config.autoPlay) play()
    }

    override fun play() {
        ruby_av_player_play(avPlayer)
        ruby_av_player_set_rate(avPlayer, playbackSpeed)
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

    override fun setVolume(value: Float) {
        require(value in 0f..1f) { "volume must be between 0 and 1" }
        volume = value
        ruby_av_player_set_volume(avPlayer, value)
        mutableSnapshots.value = snapshot(mutableSnapshots.value.state)
    }

    override fun setPlaybackSpeed(value: Float) {
        require(value > 0f) { "playbackSpeed must be greater than 0" }
        playbackSpeed = value
        ruby_av_player_set_rate(avPlayer, value)
        mutableSnapshots.value = snapshot(mutableSnapshots.value.state)
    }

    override fun setLooping(enabled: Boolean) {
        looping = enabled
        ruby_av_player_set_looping(avPlayer, enabled)
        mutableSnapshots.value = snapshot(mutableSnapshots.value.state)
    }

    override fun restart() {
        ruby_av_player_seek(avPlayer, 0.0)
        ruby_av_player_play(avPlayer)
        mutableSnapshots.value = snapshot(RubyPlaybackState.Playing, positionMs = 0L)
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
        volume = volume,
        playbackSpeed = playbackSpeed,
        looping = looping,
    )
}
