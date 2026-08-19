package io.github.waiphyoaung.rubykmpplayer

public data class RubyPlayerSnapshot(
    val state: RubyPlaybackState = RubyPlaybackState.Idle,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val volume: Float = 1f,
    val playbackSpeed: Float = 1f,
    val looping: Boolean = false,
    val errorMessage: String? = null,
)
