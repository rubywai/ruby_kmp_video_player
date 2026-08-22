# Ruby Compose Multiplatform Player

A Compose Multiplatform video player for Android and iOS. Add one dependency
to shared Compose code and call `RubyVideoPlayer`; the library creates and
releases ExoPlayer on Android and AVPlayer on iOS internally.

[Repository](https://github.com/rubywai/ruby_kmp_video_player) · Published:
`io.github.rubywai:ruby-kmp-player:1.1.1` · Next release: `1.2.0`

## Features

- Compose Multiplatform API for Android and iOS
- Progressive MP4 playback and explicit MP4 quality sources
- HLS master playlists with dynamically discovered resolutions
- Built-in `Auto` / quality selector, play, pause, mute, seek, stop, status,
  buffering, error, auto-hide, and fullscreen controls
- Optional controller API for advanced integrations

## Quick start

The examples below use `1.2.0`, which is currently tested from Maven Local and
will be available from Maven Central after the `v1.2.0` release is published.

<details open>
<summary><strong>Shared Compose code · build.gradle.kts</strong></summary>

```kotlin
commonMain.dependencies {
    implementation("io.github.rubywai:ruby-kmp-player:1.2.0")
}
```
</details>

<details open>
<summary><strong>Shared Compose code · App.kt</strong></summary>

```kotlin
@Composable
fun App() {
    RubyVideoPlayer(
        url = "https://example.com/video.mp4",
    )
}
```
</details>

The consuming app does not create a platform controller or add source files in
`androidMain`, `iosMain`, or `nativeInterop`.

## Platform setup

<details>
<summary><strong>Android · AndroidManifest.xml</strong></summary>

Remote playback requires the internet permission:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />

    <application>
        <!-- application components -->
    </application>
</manifest>
```

Use HTTPS video URLs. Cleartext HTTP is blocked by default on modern Android
versions; if it is unavoidable, add a narrowly scoped Network Security
Configuration for the required domain.

For the built-in fullscreen button, avoid recreating the activity when the
orientation changes:

```xml
<activity
    android:name=".MainActivity"
    android:configChanges="orientation|screenSize"
    android:exported="true" />
```
</details>

<details>
<summary><strong>iOS · Info.plist</strong></summary>

HTTPS URLs work with the default App Transport Security policy. If a server is
HTTP-only, add a domain-specific ATS exception; avoid broad
`NSAllowsArbitraryLoads` exceptions.

To use the built-in fullscreen button, support portrait and landscape:

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

The published iOS artifact contains the AVPlayer implementation and bridge.
The Xcode application target must link Apple's `CoreMedia.framework` under
**General → Frameworks, Libraries, and Embedded Content**.
</details>

## Player configuration

```kotlin
RubyVideoPlayer(
    url = "https://example.com/video.mp4",
    modifier = Modifier.fillMaxWidth().height(240.dp),
    config = RubyPlayerConfig(
        autoPlay = true,
        looping = false,
        startPositionMs = 0L,
        volume = 1f,
        playbackSpeed = 1f,
    ),
    controls = RubyPlayerControls(
        showPlayPause = true,
        showMute = true,
        showSeekBar = true,
        showStop = false,
        showQualitySelector = true,
        showFullscreen = true,
        showStatus = true,
        autoHide = true,
        autoHideDelayMillis = 5_000L,
    ),
)
```

`volume` must be from `0f` to `1f`; `playbackSpeed` must be greater than
`0f`; and `startPositionMs` must not be negative. Set
`showFullscreen = false` when the app manages fullscreen itself, or
`showQualitySelector = false` to hide the built-in quality menu.

## Resolutions and quality switching

### MP4 sources

Use `RubyVideoSourceSet` when the app owns separate progressive URLs. The
selected source changes while preserving playback position and runtime settings.

```kotlin
val qualities = RubyVideoSourceSet(
    qualities = listOf(
        RubyVideoQuality("240p", RubyVideoSource("https://cdn.example.com/video-240.mp4")),
        RubyVideoQuality("360p", RubyVideoSource("https://cdn.example.com/video-360.mp4")),
        RubyVideoQuality("720p", RubyVideoSource("https://cdn.example.com/video-720.mp4")),
    ),
    initialQualityLabel = "360p",
)

RubyVideoPlayer(sources = qualities)
```

Labels are exact. If `initialQualityLabel` is null, missing, or misspelled,
the first quality is selected.

### HLS master playlist

Provide one master playlist URL. Do not enumerate individual rendition URLs.
After the playlist loads, the quality menu shows `Auto` and every discovered
resolution.

```kotlin
RubyVideoPlayer(
    url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
)
```

Choose a resolution to constrain playback to that rendition. Choose `Auto` to
restore adaptive bitrate playback. Android pins the selected HLS track; iOS
uses the selected resolution as AVPlayer's maximum, so AVPlayer can choose a
lower rendition when network conditions require it.

## Advanced controller API

For custom headers, direct commands, or observing `StateFlow` snapshots, keep
using the controller-based API:

```kotlin
val controller: RubyVideoPlayerController = /* platform controller */
controller.load(
    RubyVideoSource(
        url = "https://example.com/video.mp4",
        headers = mapOf("Authorization" to "Bearer token"),
    ),
)
controller.setPlaybackSpeed(1.25f)
```

Most Compose Multiplatform apps should use the controller-free
`RubyVideoPlayer(url = ...)` or `RubyVideoPlayer(sources = ...)` overloads.

## Example application

The repository's `example/` app consumes the Maven-coordinate dependency from
shared Compose code. It includes MP4, HLS master-playlist, and MP4 quality
source screens.

```bash
./gradlew -I build/local-repo.init.gradle :example:androidApp:installDebug
./gradlew :example:composeApp:assembleDebugXCFramework
```

For the Xcode host instructions, see [iosApp/README.md](iosApp/README.md).
