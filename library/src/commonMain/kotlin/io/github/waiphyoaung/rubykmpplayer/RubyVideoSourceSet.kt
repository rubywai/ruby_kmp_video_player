package io.github.waiphyoaung.rubykmpplayer

/** A labelled progressive video source that can be selected at runtime. */
public data class RubyVideoQuality(
    val label: String,
    val source: RubyVideoSource,
) {
    init {
        require(label.isNotBlank()) { "quality label must not be blank" }
    }
}

/**
 * Ordered alternative video sources, typically different MP4 resolutions.
 *
 * [initialQualityLabel] is matched exactly. A missing or unknown label falls
 * back to the first quality in [qualities]. HLS master playlists should be
 * loaded as a single [RubyVideoSource] so the platform player can adapt
 * automatically.
 */
public data class RubyVideoSourceSet(
    val qualities: List<RubyVideoQuality>,
    val initialQualityLabel: String? = null,
) {
    init {
        require(qualities.isNotEmpty()) { "qualities must not be empty" }
        require(qualities.map(RubyVideoQuality::label).distinct().size == qualities.size) {
            "quality labels must be unique"
        }
    }

    public fun initialQuality(): RubyVideoQuality =
        qualities.firstOrNull { it.label == initialQualityLabel } ?: qualities.first()
}
