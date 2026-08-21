package io.github.waiphyoaung.rubykmpplayer

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
public class RubyAndroidVideoPlayerController(
    context: Context,
) : RubyVideoPlayerController {
    private val appContext = context.applicationContext
    // Media3 requires all player reads and writes on its application thread.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableSnapshots = MutableStateFlow(RubyPlayerSnapshot())
    private var progressJob: Job? = null
    private var userPaused = false
    private var volume = 1f
    private var muted = false
    private var playbackSpeed = 1f
    private var looping = false

    public val exoPlayer: ExoPlayer = ExoPlayer.Builder(appContext).build()

    override val snapshots: StateFlow<RubyPlayerSnapshot> = mutableSnapshots

    init {
        exoPlayer.addListener(
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    publishSnapshot()
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    publishSnapshot()
                    if (isPlaying) {
                        startProgressUpdates()
                    } else {
                        stopProgressUpdates()
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    mutableSnapshots.value = currentSnapshot(
                        state = RubyPlaybackState.Error,
                        errorMessage = error.message,
                    )
                }
            },
        )
    }

    override fun load(source: RubyVideoSource, config: RubyPlayerConfig) {
        scope.launch {
            loadOnMain(source, config)
        }
    }

    private fun loadOnMain(source: RubyVideoSource, config: RubyPlayerConfig) {
        mutableSnapshots.value = RubyPlayerSnapshot(state = RubyPlaybackState.Loading)
        userPaused = false
        volume = config.volume
        muted = false
        playbackSpeed = config.playbackSpeed
        looping = config.looping
        exoPlayer.repeatMode = if (looping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        exoPlayer.volume = if (muted) 0f else volume
        exoPlayer.setPlaybackSpeed(playbackSpeed)

        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(source.headers)
        val mediaSourceFactory = DefaultMediaSourceFactory(appContext)
            .setDataSourceFactory(dataSourceFactory)
        val mediaSource = mediaSourceFactory.createMediaSource(source.toMediaItem())

        exoPlayer.setMediaSource(mediaSource)
        if (config.startPositionMs > 0L) {
            exoPlayer.seekTo(config.startPositionMs)
        }
        exoPlayer.prepare()
        if (config.autoPlay) {
            playOnMain()
        }
        publishSnapshot()
    }

    override fun play() {
        scope.launch {
            playOnMain()
        }
    }

    private fun playOnMain() {
        userPaused = false
        exoPlayer.play()
        publishSnapshot()
    }

    override fun pause() {
        scope.launch {
            pauseOnMain()
        }
    }

    private fun pauseOnMain() {
        userPaused = true
        exoPlayer.pause()
        mutableSnapshots.value = currentSnapshot(state = RubyPlaybackState.Paused)
    }

    override fun stop() {
        scope.launch {
            stopOnMain()
        }
    }

    private fun stopOnMain() {
        userPaused = false
        exoPlayer.stop()
        stopProgressUpdates()
        mutableSnapshots.value = RubyPlayerSnapshot()
    }

    override fun seekTo(positionMs: Long) {
        scope.launch {
            exoPlayer.seekTo(positionMs.coerceAtLeast(0L))
            publishSnapshot()
        }
    }

    override fun setVolume(value: Float) {
        require(value in 0f..1f) { "volume must be between 0 and 1" }
        scope.launch {
            volume = value
            exoPlayer.volume = if (muted) 0f else value
            publishSnapshot()
        }
    }

    override fun setMuted(enabled: Boolean) {
        scope.launch {
            muted = enabled
            exoPlayer.volume = if (enabled) 0f else volume
            publishSnapshot()
        }
    }

    override fun setPlaybackSpeed(value: Float) {
        require(value > 0f) { "playbackSpeed must be greater than 0" }
        scope.launch {
            playbackSpeed = value
            exoPlayer.setPlaybackSpeed(value)
            publishSnapshot()
        }
    }

    override fun setLooping(enabled: Boolean) {
        scope.launch {
            looping = enabled
            exoPlayer.repeatMode = if (enabled) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            publishSnapshot()
        }
    }

    override fun restart() {
        scope.launch {
            userPaused = false
            exoPlayer.seekTo(0L)
            exoPlayer.play()
            publishSnapshot()
        }
    }

    override fun release() {
        scope.launch {
            stopOnMain()
            exoPlayer.release()
            scope.cancel()
            mutableSnapshots.value = RubyPlayerSnapshot()
        }
    }

    private fun RubyVideoSource.toMediaItem(): MediaItem {
        val mimeType = when (contentType) {
            RubyVideoContentType.Hls -> MimeTypes.APPLICATION_M3U8
            RubyVideoContentType.Progressive,
            RubyVideoContentType.Auto,
            -> null
        }

        return MediaItem.Builder()
            .setUri(url)
            .setMimeType(mimeType)
            .build()
    }

    private fun startProgressUpdates() {
        if (progressJob?.isActive == true) return
        progressJob = scope.launch {
            while (isActive) {
                publishSnapshot()
                delay(PROGRESS_UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun publishSnapshot() {
        mutableSnapshots.value = currentSnapshot(state = exoPlayer.toRubyState())
    }

    private fun currentSnapshot(
        state: RubyPlaybackState,
        errorMessage: String? = null,
    ): RubyPlayerSnapshot = RubyPlayerSnapshot(
        state = state,
        durationMs = exoPlayer.duration.sanitizeTime(),
        positionMs = exoPlayer.currentPosition.sanitizeTime(),
        bufferedPositionMs = exoPlayer.bufferedPosition.sanitizeTime(),
        volume = volume,
        muted = muted,
        playbackSpeed = playbackSpeed,
        looping = looping,
        errorMessage = errorMessage,
    )

    private fun ExoPlayer.toRubyState(): RubyPlaybackState = when {
        playbackState == Player.STATE_BUFFERING -> RubyPlaybackState.Buffering
        playbackState == Player.STATE_ENDED -> RubyPlaybackState.Ended
        playbackState == Player.STATE_IDLE -> RubyPlaybackState.Idle
        isPlaying -> RubyPlaybackState.Playing
        playbackState == Player.STATE_READY && userPaused -> RubyPlaybackState.Paused
        playbackState == Player.STATE_READY -> RubyPlaybackState.Ready
        else -> RubyPlaybackState.Idle
    }

    private fun Long.sanitizeTime(): Long =
        if (this == C.TIME_UNSET || this < 0L) 0L else this

    private companion object {
        const val PROGRESS_UPDATE_INTERVAL_MS = 500L
    }
}
