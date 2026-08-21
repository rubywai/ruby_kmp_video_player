package io.github.waiphyoaung.rubykmpplayer

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.AVFoundation.AVPlayer
import platform.AVKit.AVPlayerViewController
import platform.Foundation.NSURL
import io.github.waiphyoaung.rubykmpplayer.bridge.ruby_av_player_load
import io.github.waiphyoaung.rubykmpplayer.bridge.ruby_av_player_configure_audio_session
import io.github.waiphyoaung.rubykmpplayer.bridge.ruby_av_player_buffered_position_seconds
import io.github.waiphyoaung.rubykmpplayer.bridge.ruby_av_player_duration_seconds
import io.github.waiphyoaung.rubykmpplayer.bridge.ruby_av_player_pause
import io.github.waiphyoaung.rubykmpplayer.bridge.ruby_av_player_position_seconds
import io.github.waiphyoaung.rubykmpplayer.bridge.ruby_av_player_item_status
import io.github.waiphyoaung.rubykmpplayer.bridge.ruby_av_player_play
import io.github.waiphyoaung.rubykmpplayer.bridge.ruby_av_player_seek
import io.github.waiphyoaung.rubykmpplayer.bridge.ruby_av_player_set_rate
import io.github.waiphyoaung.rubykmpplayer.bridge.ruby_av_player_set_looping
import io.github.waiphyoaung.rubykmpplayer.bridge.ruby_av_player_set_volume
import io.github.waiphyoaung.rubykmpplayer.bridge.ruby_av_player_stop

@OptIn(ExperimentalForeignApi::class)
public class RubyIosVideoPlayerController : RubyVideoPlayerController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableSnapshots = MutableStateFlow(RubyPlayerSnapshot())
    private var progressJob: Job? = null
    private var volume = 1f
    private var muted = false
    private var playbackSpeed = 1f
    private var looping = false
    private var playRequested = false

    public val avPlayer: AVPlayer = AVPlayer()

    init {
        ruby_av_player_configure_audio_session()
    }

    override val snapshots: StateFlow<RubyPlayerSnapshot> = mutableSnapshots

    override fun load(source: RubyVideoSource, config: RubyPlayerConfig) {
        mutableSnapshots.value = RubyPlayerSnapshot(state = RubyPlaybackState.Loading)
        ruby_av_player_configure_audio_session()

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
        muted = false
        playbackSpeed = config.playbackSpeed
        looping = config.looping
        ruby_av_player_set_volume(avPlayer, if (muted) 0f else config.volume)
        ruby_av_player_set_rate(avPlayer, playbackSpeed)
        ruby_av_player_set_looping(avPlayer, looping)
        playRequested = config.autoPlay
        startProgressUpdates()
        if (playRequested) {
            ruby_av_player_play(avPlayer)
        }
    }

    override fun play() {
        playRequested = true
        ruby_av_player_play(avPlayer)
        ruby_av_player_set_rate(avPlayer, playbackSpeed)
        mutableSnapshots.value = snapshot(
            if (ruby_av_player_item_status(avPlayer) == AV_PLAYER_ITEM_READY) {
                RubyPlaybackState.Playing
            } else {
                RubyPlaybackState.Loading
            },
        )
    }

    override fun pause() {
        playRequested = false
        ruby_av_player_pause(avPlayer)
        mutableSnapshots.value = snapshot(RubyPlaybackState.Paused)
    }

    override fun stop() {
        playRequested = false
        ruby_av_player_stop(avPlayer)
        stopProgressUpdates()
        mutableSnapshots.value = RubyPlayerSnapshot()
    }

    override fun seekTo(positionMs: Long) {
        ruby_av_player_seek(avPlayer, positionMs / 1_000.0)
        mutableSnapshots.value = snapshot(mutableSnapshots.value.state, positionMs = positionMs)
    }

    override fun setVolume(value: Float) {
        require(value in 0f..1f) { "volume must be between 0 and 1" }
        volume = value
        ruby_av_player_set_volume(avPlayer, if (muted) 0f else value)
        mutableSnapshots.value = snapshot(mutableSnapshots.value.state)
    }

    override fun setMuted(enabled: Boolean) {
        muted = enabled
        ruby_av_player_set_volume(avPlayer, if (enabled) 0f else volume)
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
        playRequested = true
        ruby_av_player_seek(avPlayer, 0.0)
        ruby_av_player_play(avPlayer)
        mutableSnapshots.value = snapshot(RubyPlaybackState.Playing, positionMs = 0L)
    }

    override fun release() {
        stop()
        scope.cancel()
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
        durationMs = ruby_av_player_duration_seconds(avPlayer).toMilliseconds(),
        positionMs = if (positionMs == mutableSnapshots.value.positionMs) {
            ruby_av_player_position_seconds(avPlayer).toMilliseconds()
        } else {
            positionMs
        },
        bufferedPositionMs = ruby_av_player_buffered_position_seconds(avPlayer).toMilliseconds(),
        volume = volume,
        muted = muted,
        playbackSpeed = playbackSpeed,
        looping = looping,
    )

    private fun startProgressUpdates() {
        if (progressJob?.isActive == true) return
        progressJob = scope.launch {
            while (isActive) {
                val current = mutableSnapshots.value
                val itemReady = ruby_av_player_item_status(avPlayer) == AV_PLAYER_ITEM_READY
                val state = when {
                    current.state == RubyPlaybackState.Error -> RubyPlaybackState.Error
                    !itemReady -> RubyPlaybackState.Loading
                    playRequested -> RubyPlaybackState.Playing
                    current.state == RubyPlaybackState.Paused -> RubyPlaybackState.Paused
                    else -> RubyPlaybackState.Ready
                }
                mutableSnapshots.value = snapshot(state)
                delay(PROGRESS_UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun Double.toMilliseconds(): Long =
        if (isFinite() && this > 0.0) (this * 1_000.0).toLong() else 0L

    private companion object {
        const val AV_PLAYER_ITEM_READY = 1
        const val PROGRESS_UPDATE_INTERVAL_MS = 250L
    }
}
