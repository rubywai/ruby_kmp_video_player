package io.github.waiphyoaung.rubykmpplayer

public enum class RubyVideoContentType {
    Auto,
    Progressive,
    Hls,
}

public data class RubyVideoSource(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val contentType: RubyVideoContentType = RubyVideoContentType.Auto,
)
