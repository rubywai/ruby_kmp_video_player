package io.github.waiphyoaung.rubykmpplayer

public data class RubyPlayerControls(
    val showPlayPause: Boolean = true,
    val showMute: Boolean = true,
    val showSeekBar: Boolean = true,
    val showStop: Boolean = true,
    val showQualitySelector: Boolean = true,
    val showFullscreen: Boolean = true,
    val showStatus: Boolean = true,
    val autoHide: Boolean = true,
    val autoHideDelayMillis: Long = 5_000L,
)
