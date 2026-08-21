# Ruby KMP Player

Ruby KMP Player is a reusable Kotlin Multiplatform video player library.

Repository: https://github.com/rubywai/ruby_kmp_video_player

Current scope:

- Android playback through AndroidX Media3 ExoPlayer
- iOS playback through AVPlayer
- URL video sources
- play, pause, stop, seek, and release controls
- observable playback snapshots with `StateFlow`
- shared Compose Multiplatform player UI
- built-in play/pause, mute, seek bar, stop, status, buffering, error, and fullscreen controls
- configurable control visibility

Advanced Better Player style features such as cache, DRM, subtitles, playlist,
Picture in Picture, notifications, and fully custom control slots are not part of v1.

## Common API

Add the library to the shared source set of your Compose Multiplatform app:

```kotlin
commonMain.dependencies {
    implementation("io.github.rubywai:ruby-kmp-player:1.1.1")
}
```

Then play a URL directly from shared Compose code. The library creates, loads,
observes, and releases the Android or iOS player internally.

```kotlin
@Composable
fun App() {
    RubyVideoPlayer(
        url = "https://example.com/video.mp4",
    )
}
```

No platform controller, `androidMain`, `iosMain`, or native interop code is
required in the consuming application.

### Configuration

```kotlin
val source = RubyVideoSource(
    url = "https://example.com/video.mp4",
)

val config = RubyPlayerConfig(
    autoPlay = true,
    looping = false,
    startPositionMs = 0L,
    volume = 1f,
    playbackSpeed = 1f,
)
```

`volume` must be between `0f` and `1f`. `playbackSpeed` must be greater than
`0f`, and `startPositionMs` must not be negative. These options are applied by
the Android ExoPlayer and iOS AVPlayer implementations.

The controller API remains available for advanced integrations that need
direct playback commands, custom headers, or snapshot observation.

```kotlin
interface RubyVideoPlayerController {
    val snapshots: StateFlow<RubyPlayerSnapshot>

    fun load(source: RubyVideoSource, config: RubyPlayerConfig = RubyPlayerConfig())
    fun play()
    fun pause()
    fun stop()
    fun seekTo(positionMs: Long)
    fun setVolume(value: Float)
    fun setMuted(enabled: Boolean)
    fun setPlaybackSpeed(value: Float)
    fun setLooping(enabled: Boolean)
    fun restart()
    fun release()
}
```

Playback settings can also be changed while the current item is loaded. The
controller exposes the current values through `RubyPlayerSnapshot`.

```kotlin
controller.setVolume(0.5f)
controller.setPlaybackSpeed(1.25f)
controller.setLooping(true)
controller.restart()
```

## Shared Compose Player

The URL overload can be configured entirely from shared code:

```kotlin
RubyVideoPlayer(
    url = "https://example.com/video.mp4",
    modifier = Modifier.fillMaxWidth().height(240.dp),
    config = RubyPlayerConfig(autoPlay = true),
    controls = RubyPlayerControls(
        showPlayPause = true,
        showMute = true,
        showSeekBar = true,
        showStop = false,
        showFullscreen = true,
        showStatus = true,
        autoHide = true,
        autoHideDelayMillis = 5_000L,
    ),
)
```

Advanced users can pass a caller-managed `RubyVideoPlayerController` to the
existing controller overload instead.

When enabled, controls hide after five seconds while playback is active. A
tap on the video shows or hides them, and any control interaction resets the
timer. Controls remain visible while loading, paused, or displaying an error.

### Fullscreen and controls

Fullscreen is enabled by default. The player opens a borderless fullscreen
dialog, hides the Android system bars, and requests landscape orientation on
Android and iOS. Closing fullscreen restores the previous Android system UI
and orientation and requests portrait orientation on iOS.

```kotlin
RubyVideoPlayer(
    url = "https://example.com/video.mp4",
    controls = RubyPlayerControls(
        showFullscreen = true,
        showPlayPause = true,
        showMute = true,
        showSeekBar = true,
        showStop = false,
        showStatus = true,
        autoHide = true,
        autoHideDelayMillis = 5_000L,
    ),
)
```

Set `showFullscreen = false` when the host application manages fullscreen or
orientation itself. `RubyPlayerControls` controls button visibility and
auto-hide behavior; playback settings such as autoplay, looping, volume, and
speed belong in `RubyPlayerConfig`.

## Android

### Consumer setup

Remote video playback requires internet permission in the Android application
manifest, usually `androidApp/src/main/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />

    <application>
        <!-- application components -->
    </application>
</manifest>
```

To avoid recreating the activity when the built-in fullscreen control changes
orientation, let the activity handle orientation and screen-size changes:

```xml
<activity
    android:name=".MainActivity"
    android:configChanges="orientation|screenSize"
    android:exported="true" />
```

Use HTTPS video URLs. Android blocks cleartext HTTP traffic by default on
modern target SDKs; if HTTP is unavoidable, configure a narrowly scoped
Network Security Configuration in the consuming application.

The repository includes a runnable Compose Multiplatform example under
`example/`. The Android launcher is `:example:androidApp`, and the shared
Compose application is `:example:composeApp`.

```bash
./gradlew :example:androidApp:installDebug
```

For advanced controller-based use on Android:

```kotlin
val controller = RubyAndroidVideoPlayerController(context)
controller.load(source, config)
```

## Compose Multiplatform Example

The Android and iOS examples use the same library-provided `RubyVideoPlayer`
composable from shared code. The library supplies the native video surface
through platform implementations.

Build the Android example with:

```bash
./gradlew :example:androidApp:installDebug
```

Build the iOS simulator framework with:

```bash
./gradlew :example:composeApp:linkDebugFrameworkIosSimulatorArm64
```

Then follow [`iosApp/README.md`](iosApp/README.md) to open the
Xcode host app and run the generated framework.

## iOS

### Requirements

- macOS with Xcode installed and selected as the active developer directory
- Xcode command-line tools available through `xcodebuild` and `xcrun`
- An iOS 15 or newer SDK
- A compatible iOS Simulator runtime when running on Simulator
- An Apple Developer team and a registered device when running on a physical iPhone or iPad

### Consumer setup

HTTPS video URLs work with the default App Transport Security policy. Plain
HTTP URLs require an appropriate domain-specific ATS exception in the
consumer's `Info.plist`; broad `NSAllowsArbitraryLoads` exceptions are not
recommended.

For the built-in fullscreen button to rotate the player, include portrait and
landscape orientations in the iOS application `Info.plist`:

```xml
<key>UISupportedInterfaceOrientations</key>
<array>
    <string>UIInterfaceOrientationPortrait</string>
    <string>UIInterfaceOrientationLandscapeLeft</string>
    <string>UIInterfaceOrientationLandscapeRight</string>
</array>
<key>UISupportedInterfaceOrientations~ipad</key>
<array>
    <string>UIInterfaceOrientationPortrait</string>
    <string>UIInterfaceOrientationLandscapeLeft</string>
    <string>UIInterfaceOrientationLandscapeRight</string>
</array>
```

If the host app intentionally supports portrait only, set
`RubyPlayerControls(showFullscreen = false)` and manage fullscreen in the host.
Consumers do not need to add native interop files; the published iOS artifacts
contain the AVPlayer implementation and bridge.

The project uses a manually maintained Xcode host app because the shared
Compose framework is generated by Gradle. Build the framework before opening
the Xcode project:

```bash
./gradlew :example:composeApp:assembleDebugXCFramework --no-configuration-cache
```

Then open `iosApp/RubyKmpPlayerExample.xcodeproj`, select the
`RubyKmpPlayerExample` scheme, choose a simulator or connected device, and run.

If Xcode reports that no compatible Simulator runtime exists for the selected
SDK, install the matching platform in Xcode or run:

```bash
xcodebuild -downloadPlatform iOS
```

For a physical device, select your Apple Developer team under the Xcode target
Signing & Capabilities settings. The example requests network access because
the default video is loaded from an HTTPS URL.

For advanced controller-based use on iOS, the implementation uses AVPlayer:

```kotlin
val controller = RubyIosVideoPlayerController()
controller.load(source, config)
```

The shared Compose controls own play/pause, seeking, status, buffering, error,
fullscreen, and auto-hide behavior. Native AVPlayer playback controls are
disabled to avoid duplicate controls.
