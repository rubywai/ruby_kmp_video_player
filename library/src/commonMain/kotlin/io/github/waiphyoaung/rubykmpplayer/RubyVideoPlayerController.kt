package io.github.waiphyoaung.rubykmpplayer

import kotlinx.coroutines.flow.StateFlow

public interface RubyVideoPlayerController {
    public val snapshots: StateFlow<RubyPlayerSnapshot>

    public fun load(
        source: RubyVideoSource,
        config: RubyPlayerConfig = RubyPlayerConfig(),
    )

    public fun play()

    public fun pause()

    public fun stop()

    public fun seekTo(positionMs: Long)

    public fun setVolume(value: Float)

    public fun setPlaybackSpeed(value: Float)

    public fun setLooping(enabled: Boolean)

    public fun restart()

    public fun release()
}
