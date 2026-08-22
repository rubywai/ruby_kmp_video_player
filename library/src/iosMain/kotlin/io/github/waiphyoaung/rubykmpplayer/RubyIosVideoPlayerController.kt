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
import io.github.waiphyoaung.rubykmpplayer.bridge.ruby_av_player_hls_variant_count
import io.github.waiphyoaung.rubykmpplayer.bridge.ruby_av_player_hls_variant_height
import io.github.waiphyoaung.rubykmpplayer.bridge.ruby_av_player_hls_variant_peak_bitrate
import io.github.waiphyoaung.rubykmpplayer.bridge.ruby_av_player_hls_variant_width
import io.github.waiphyoaung.rubykmpplayer.bridge.ruby_av_player_play
import io.github.waiphyoaung.rubykmpplayer.bridge.ruby_av_player_seek
import io.github.waiphyoaung.rubykmpplayer.bridge.ruby_av_player_set_rate
import io.github.waiphyoaung.rubykmpplayer.bridge.ruby_av_player_set_looping
import io.github.waiphyoaung.rubykmpplayer.bridge.ruby_av_player_set_volume
import io.github.waiphyoaung.rubykmpplayer.bridge.ruby_av_player_select_hls_auto
import io.github.waiphyoaung.rubykmpplayer.bridge.ruby_av_player_select_hls_variant
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
    private var qualities: List<RubyVideoQuality> = emptyList()
    private var selectedQualityLabel: String? = null
    private var hlsQualities: Map<String, RubyHlsQuality> = emptyMap()
    private var isHlsSource = false

    public val avPlayer: AVPlayer = AVPlayer()

    init {
        ruby_av_player_configure_audio_session()
    }

    override val snapshots: StateFlow<RubyPlayerSnapshot> = mutableSnapshots

    override fun load(source: RubyVideoSource, config: RubyPlayerConfig) {
        qualities = emptyList()
        selectedQualityLabel = null
        hlsQualities = emptyMap()
        isHlsSource = source.isHls()
        loadSource(source, config, resetMuted = true)
    }

    override fun load(sources: RubyVideoSourceSet, config: RubyPlayerConfig) {
        val initialQuality = sources.initialQuality()
        qualities = sources.qualities
        selectedQualityLabel = initialQuality.label
        hlsQualities = emptyMap()
        isHlsSource = false
        loadSource(initialQuality.source, config, resetMuted = true)
    }

    override fun selectQuality(label: String) {
        val quality = qualities.firstOrNull { it.label == label }
        if (quality != null) {
            if (quality.label == selectedQualityLabel) return

            val positionMs = mutableSnapshots.value.positionMs
            val wasPlaying = playRequested
            selectedQualityLabel = quality.label
            loadSource(
                source = quality.source,
                config = RubyPlayerConfig(
                    autoPlay = wasPlaying,
                    looping = looping,
                    startPositionMs = positionMs,
                    volume = volume,
                    playbackSpeed = playbackSpeed,
                ),
                resetMuted = false,
            )
            return
        }

        if (!isHlsSource || (label != HLS_AUTO_LABEL && label !in hlsQualities)) return
        if (label == HLS_AUTO_LABEL) {
            ruby_av_player_select_hls_auto(avPlayer)
        } else {
            val hlsQuality = checkNotNull(hlsQualities[label])
            ruby_av_player_select_hls_variant(
                avPlayer,
                hlsQuality.width,
                hlsQuality.height,
                hlsQuality.peakBitrate,
            )
        }
        selectedQualityLabel = label
        mutableSnapshots.value = snapshot(mutableSnapshots.value.state)
    }

    private fun loadSource(
        source: RubyVideoSource,
        config: RubyPlayerConfig,
        resetMuted: Boolean,
    ) {
        mutableSnapshots.value = loadingSnapshot()
        ruby_av_player_configure_audio_session()

        val url = NSURL.URLWithString(source.url)
        if (url == null) {
            mutableSnapshots.value = loadingSnapshot().copy(
                state = RubyPlaybackState.Error,
                errorMessage = "Invalid video URL: ${source.url}",
            )
            return
        }

        ruby_av_player_load(avPlayer, url)
        if (isHlsSource) {
            selectedQualityLabel = HLS_AUTO_LABEL
        }
        volume = config.volume
        if (resetMuted) muted = false
        playbackSpeed = config.playbackSpeed
        looping = config.looping
        ruby_av_player_set_volume(avPlayer, if (muted) 0f else config.volume)
        ruby_av_player_set_rate(avPlayer, playbackSpeed)
        ruby_av_player_set_looping(avPlayer, looping)
        if (config.startPositionMs > 0L) {
            ruby_av_player_seek(avPlayer, config.startPositionMs / 1_000.0)
        }
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
        availableQualityLabels = availableQualityLabels(),
        selectedQualityLabel = selectedQualityLabel,
    )

    private fun loadingSnapshot(): RubyPlayerSnapshot = RubyPlayerSnapshot(
        state = RubyPlaybackState.Loading,
        volume = volume,
        muted = muted,
        playbackSpeed = playbackSpeed,
        looping = looping,
        availableQualityLabels = availableQualityLabels(),
        selectedQualityLabel = selectedQualityLabel,
    )

    private fun startProgressUpdates() {
        if (progressJob?.isActive == true) return
        progressJob = scope.launch {
            while (isActive) {
                val current = mutableSnapshots.value
                val itemReady = ruby_av_player_item_status(avPlayer) == AV_PLAYER_ITEM_READY
                discoverHlsQualities()
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

    private fun discoverHlsQualities() {
        if (!isHlsSource || qualities.isNotEmpty() || hlsQualities.isNotEmpty()) return

        hlsQualities = (0 until ruby_av_player_hls_variant_count(avPlayer))
            .mapNotNull { index ->
                val width = ruby_av_player_hls_variant_width(avPlayer, index)
                val height = ruby_av_player_hls_variant_height(avPlayer, index)
                if (width > 0 && height > 0) {
                    "${height}p" to RubyHlsQuality(
                        width = width,
                        height = height,
                        peakBitrate = ruby_av_player_hls_variant_peak_bitrate(avPlayer, index),
                    )
                } else {
                    null
                }
            }
            .sortedBy { (label, _) -> label.removeSuffix("p").toInt() }
            .toMap()
        if (hlsQualities.isNotEmpty() && selectedQualityLabel == null) {
            selectedQualityLabel = HLS_AUTO_LABEL
        }
    }

    private fun availableQualityLabels(): List<String> = when {
        qualities.isNotEmpty() -> qualities.map(RubyVideoQuality::label)
        hlsQualities.isNotEmpty() -> listOf(HLS_AUTO_LABEL) + hlsQualities.keys
        else -> emptyList()
    }

    private fun RubyVideoSource.isHls(): Boolean =
        contentType == RubyVideoContentType.Hls ||
            (contentType == RubyVideoContentType.Auto && url.substringBefore('?').endsWith(".m3u8", ignoreCase = true))

    private data class RubyHlsQuality(
        val width: Int,
        val height: Int,
        val peakBitrate: Double,
    )

    private fun Double.toMilliseconds(): Long =
        if (isFinite() && this > 0.0) (this * 1_000.0).toLong() else 0L

    private companion object {
        const val AV_PLAYER_ITEM_READY = 1
        const val PROGRESS_UPDATE_INTERVAL_MS = 250L
        const val HLS_AUTO_LABEL = "Auto"
    }
}
