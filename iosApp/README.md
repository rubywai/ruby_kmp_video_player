# iOS Example

## Requirements

- macOS with Xcode installed
- Xcode command-line tools available through `xcodebuild` and `xcrun`
- An iOS 15 or newer SDK
- A compatible iOS Simulator runtime for the selected Xcode SDK
- An Apple Developer team and a registered device for physical-device runs

The project uses a manually maintained Xcode host app around the shared Compose
Multiplatform framework.

## Build The Framework

Run this command from the repository root:

```bash
./gradlew :example:composeApp:assembleDebugXCFramework --no-configuration-cache
```

The generated framework is:

```text
example/composeApp/build/xcode-frameworks/debug/RubyExampleComposeApp.xcframework
```

The example consumes `io.github.rubywai:ruby-kmp-player:1.1.1` from Maven
Central. The published iOS artifact contains its AVPlayer implementation and
native bridge, so the application does not provide native interop files or
link a separate bridge archive. The iOS host target links Apple's `CoreMedia`
system framework, which the embedded bridge uses for playback timing.

## Run In Xcode

1. Build the XCFramework from the repository root.
2. Open `iosApp/RubyKmpPlayerExample.xcodeproj`.
3. Select the `RubyKmpPlayerExample` scheme.
4. Select an iOS Simulator or connected physical device.
5. For a physical device, select your Apple Developer team in Signing & Capabilities.
6. Run the app and tap **Load video**.

The XCFramework includes both the device and simulator binaries. Rebuild it
after changing shared Kotlin or library code before running the Xcode host.

If Xcode reports that no compatible Simulator runtime exists for the selected
SDK, install the matching runtime in Xcode or run:

```bash
xcodebuild -downloadPlatform iOS
```

The example uses an HTTPS video URL and requires network access. The shared
Compose controls provide play/pause, seeking, status, buffering, error, and
fullscreen behavior; native AVPlayer playback controls are disabled to avoid
duplicate seek bars and buttons.
