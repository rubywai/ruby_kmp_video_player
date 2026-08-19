package io.github.waiphyoaung.rubykmpplayer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.test.Test
import kotlin.test.assertEquals

class FakeRubyVideoPlayerControllerTest {
    @Test
    fun fakeControllerModelsBasicPlaybackTransitions() {
        val controller = FakeRubyVideoPlayerController()

        controller.load(RubyVideoSource("https://example.com/video.mp4"))
        assertEquals(RubyPlaybackState.Ready, controller.snapshots.value.state)

        controller.play()
        assertEquals(RubyPlaybackState.Playing, controller.snapshots.value.state)

        controller.pause()
        assertEquals(RubyPlaybackState.Paused, controller.snapshots.value.state)

        controller.seekTo(1_000L)
        assertEquals(1_000L, controller.snapshots.value.positionMs)

        controller.setVolume(0.5f)
        controller.setPlaybackSpeed(1.5f)
        controller.setLooping(true)
        assertEquals(0.5f, controller.snapshots.value.volume)
        assertEquals(1.5f, controller.snapshots.value.playbackSpeed)
        assertEquals(true, controller.snapshots.value.looping)

        controller.restart()
        assertEquals(0L, controller.snapshots.value.positionMs)
        assertEquals(RubyPlaybackState.Playing, controller.snapshots.value.state)

        controller.stop()
        assertEquals(RubyPlaybackState.Idle, controller.snapshots.value.state)
    }
}

private class FakeRubyVideoPlayerController : RubyVideoPlayerController {
    private val mutableSnapshots = MutableStateFlow(RubyPlayerSnapshot())
    override val snapshots: StateFlow<RubyPlayerSnapshot> = mutableSnapshots

    override fun load(source: RubyVideoSource, config: RubyPlayerConfig) {
        mutableSnapshots.value = RubyPlayerSnapshot(
            state = if (config.autoPlay) RubyPlaybackState.Playing else RubyPlaybackState.Ready,
            positionMs = config.startPositionMs,
        )
    }

    override fun play() {
        mutableSnapshots.value = mutableSnapshots.value.copy(state = RubyPlaybackState.Playing)
    }

    override fun pause() {
        mutableSnapshots.value = mutableSnapshots.value.copy(state = RubyPlaybackState.Paused)
    }

    override fun stop() {
        mutableSnapshots.value = RubyPlayerSnapshot()
    }

    override fun seekTo(positionMs: Long) {
        mutableSnapshots.value = mutableSnapshots.value.copy(positionMs = positionMs)
    }

    override fun setVolume(value: Float) {
        mutableSnapshots.value = mutableSnapshots.value.copy(volume = value)
    }

    override fun setPlaybackSpeed(value: Float) {
        mutableSnapshots.value = mutableSnapshots.value.copy(playbackSpeed = value)
    }

    override fun setLooping(enabled: Boolean) {
        mutableSnapshots.value = mutableSnapshots.value.copy(looping = enabled)
    }

    override fun restart() {
        mutableSnapshots.value = mutableSnapshots.value.copy(
            state = RubyPlaybackState.Playing,
            positionMs = 0L,
        )
    }

    override fun release() {
        stop()
    }
}
