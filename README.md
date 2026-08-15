# Ruby KMP Player

Ruby KMP Player is a reusable Kotlin Multiplatform video player library.

Repository: https://github.com/rubywai/ruby_kmp_video_player

The first version intentionally stays small:

- Android playback through AndroidX Media3 ExoPlayer
- iOS native player handle through AVPlayer
- URL video sources
- play, pause, stop, seek, and release controls
- observable playback snapshots with `StateFlow`
- native player view access on Android and iOS

Advanced Better Player style features such as cache, DRM, subtitles, playlist,
Picture in Picture, notifications, and fully custom controls are not part of v1.

## Common API

```kotlin
val source = RubyVideoSource(
    url = "https://example.com/video.mp4",
)

val config = RubyPlayerConfig(
    autoPlay = true,
    looping = false,
)
```

Shared application code should depend on the common `RubyVideoPlayerController`
interface and observe `snapshots`.

```kotlin
interface RubyVideoPlayerController {
    val snapshots: StateFlow<RubyPlayerSnapshot>

    fun load(source: RubyVideoSource, config: RubyPlayerConfig = RubyPlayerConfig())
    fun play()
    fun pause()
    fun stop()
    fun seekTo(positionMs: Long)
    fun release()
}
```

## Android

The repository includes a runnable Android example in `:example`.

```bash
./gradlew :example:installDebug
```

Create a controller with an Android `Context`, then bind it to
`RubyAndroidPlayerView`.

```kotlin
val controller = RubyAndroidVideoPlayerController(context)
playerView.bind(controller)
controller.load(source, config)
```

## iOS

Create a controller and use its `AVPlayerViewController` helper when you need a
native player UI. The iOS controller currently provides the native AVPlayer
surface and shared state contract; selector-level playback wiring will be added
as the iOS adapter is expanded.

```kotlin
val controller = RubyIosVideoPlayerController()
val viewController = controller.createPlayerViewController()
controller.load(source, config)
```

## Roadmap

Planned future additions:

- HLS-specific polish
- subtitles
- playlist support
- cache
- Picture in Picture
- DRM
- custom Compose Multiplatform controls
