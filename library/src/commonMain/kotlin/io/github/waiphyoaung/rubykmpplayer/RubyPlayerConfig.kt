package io.github.waiphyoaung.rubykmpplayer

public data class RubyPlayerConfig(
    val autoPlay: Boolean = false,
    val looping: Boolean = false,
    val startPositionMs: Long = 0L,
    val volume: Float = 1f,
    val playbackSpeed: Float = 1f,
) {
    init {
        require(volume in 0f..1f) { "volume must be between 0 and 1" }
        require(playbackSpeed > 0f) { "playbackSpeed must be greater than 0" }
        require(startPositionMs >= 0L) { "startPositionMs must not be negative" }
    }
}
