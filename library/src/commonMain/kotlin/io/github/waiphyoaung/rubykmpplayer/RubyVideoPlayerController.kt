package io.github.waiphyoaung.rubykmpplayer

import kotlinx.coroutines.flow.StateFlow

public interface RubyVideoPlayerController {
    public val snapshots: StateFlow<RubyPlayerSnapshot>

    public fun load(
        source: RubyVideoSource,
        config: RubyPlayerConfig = RubyPlayerConfig(),
    )

    public fun load(
        sources: RubyVideoSourceSet,
        config: RubyPlayerConfig = RubyPlayerConfig(),
    )

    /** Switches to a labelled source from the currently loaded source set. */
    public fun selectQuality(label: String)

    public fun play()

    public fun pause()

    public fun stop()

    public fun seekTo(positionMs: Long)

    public fun setVolume(value: Float)

    public fun setMuted(enabled: Boolean)

    public fun setPlaybackSpeed(value: Float)

    public fun setLooping(enabled: Boolean)

    public fun restart()

    public fun release()
}
