# Bugsy — sample-ios

[🇧🇷 Português](README.md) · 🇺🇸 English

Sample app in **pure Swift** (no KMP) that integrates Bugsy via `Bugsy.xcframework`.

## Running

1. Generate the framework (debug):

   ```sh
   # Debug: full Bugsy
   ./gradlew :ui:linkDebugFrameworkIosSimulatorArm64 :ui:iosSimulatorArm64AggregateResources
   # Release: no-op artifact (same API, no instrumentation)
   ./gradlew :bugsy-noop:linkReleaseFrameworkIosSimulatorArm64
   ```

   The project picks the framework based on the configuration: **Debug** uses `ui/build/bin/<target>/debugFramework` and **Release** uses `bugsy-noop/build/bin/<target>/releaseFramework`. The resources task generates the overlay's strings and fonts (Debug only); without it the app may use stale resources and crash when opening the overlay. For a real device, swap `iosSimulatorArm64` for `iosArm64`.
2. Open `sample-ios/BugsySample.xcodeproj` in Xcode and run on the simulator.
3. Tap **Fazer request (URLSession.shared)** and then **Abrir Bugsy** (or shake the device: *Device > Shake* in the simulator). The request shows up in the Requests tab, with `Authorization` masked as `***`.

## Running on a real iPhone

The simulator needs no signing. For an iPhone Xcode requires an account, but **a free Apple ID is enough** (the same one
you use for iCloud); the paid Developer Program is not needed. You do this once:

1. **Xcode > Settings > Accounts > "+" > Apple ID** and sign in.
2. In the **BugsySample > Signing & Capabilities** target, pick **Team: Your Name (Personal Team)**.
3. Change the **Bundle Identifier** (`dev.bugsy.sample`) to one of your own, for example `dev.bugsy.sample.yourname`. With
   a free account Xcode usually refuses an identifier that is not yours.
4. Build the framework **for a device** (the simulator one does not work):

   ```sh
   ./gradlew :ui:linkDebugFrameworkIosArm64 :ui:iosArm64AggregateResources
   ```

5. On the iPhone, turn on **Developer Mode** (Settings > Privacy & Security; the device restarts).
6. Run from Xcode. The first time, the iPhone says the certificate is not trusted: go to **Settings > General > VPN &
   Device Management**, tap your Apple ID under "Developer App" and then **Trust**. Run again.
7. Accept the **camera** (to read the QR code) and **Local Network** (for pairing) requests.

With a free Apple ID the installed app **expires after 7 days**; just run it again from Xcode.

Xcode writes your team and identifier into `project.pbxproj`: **do not include those changes in a commit**.

### Common errors

| Message | Cause and fix |
|---|---|
| `Signing for "BugsySample" requires a development team` | No team picked: steps 1 and 2. |
| `Unable to resolve module dependency: 'Bugsy'` | The framework for the chosen destination is missing: a device uses `...IosArm64` and the simulator uses `...IosSimulatorArm64`. Build the one for your destination. |
| `The Developer App Certificate is not trusted` | The certificate is not trusted on the iPhone: step 6. |

In the **simulator** the camera and broadcast are limited: pairing by QR code and by code does not work. Use the manual
connection to the desktop.

## Shake

The `Shake` trigger works on a real device (accelerometer and iOS's shake gesture) and also in the Simulator, via **Device > Shake** (Ctrl+Cmd+Z): Bugsy listens to UIKit's motion event in addition to the accelerometer, which doesn't exist in the Simulator. The Simulator only delivers the gesture if the app is in the foreground.

## Release

In Release the app links the `Bugsy.framework` from the `:bugsy-noop` module: the same API (`Bugsy.shared.initialize`, `show`, `startBugsyIosOverlay`...), but without the interceptor, repository, overlay, or resources. Nothing is registered, captured, stored, or logged, and `show()` doesn't open anything. In the simulator, the Release binary has no `UrlProtocol`, `DebugRepository`, `FlagManager`, etc. symbols.

## Overlay resources

The overlay is Compose Multiplatform and reads strings and fonts from the app bundle's `compose-resources/`. The project has a Run Script phase ("Copy Compose Resources") that copies these files from `ui/build`. An app consuming `Bugsy.xcframework` needs an equivalent step.

## Pairing with Bugsy Desktop

Pairing scans a QR code with the camera and finds the desktop on the local network, so an app that embeds Bugsy on iOS needs two `Info.plist` entries (the sample sets them as `INFOPLIST_KEY_*` build settings):

- `NSCameraUsageDescription`: shown when the overlay asks for camera access to scan the desktop's QR code.
- `NSLocalNetworkUsageDescription`: shown by iOS the first time Bugsy sends or receives a pairing broadcast on the local network.

Without them iOS terminates the app on the first camera request, and silently blocks local-network traffic.

## Autorun mode

With `BUGSY_SAMPLE_AUTORUN=1` in the environment, the app makes the request and opens the overlay on its own after 3 s:

```sh
SIMCTL_CHILD_BUGSY_SAMPLE_AUTORUN=1 xcrun simctl launch <UDID> dev.bugsy.sample
```

## Testing a pre-release version

There's no equivalent of `sample-android`'s `-PbugsyFromMavenLocal` here yet: the project always links the framework from a local build (debug or release), never from a published/downloaded `.xcframework`. To validate a pre-release on iOS today, you need to rebuild the framework from source at the desired version/tag (see "Running" above) — there's no way to point directly at an already-published artifact.

## Integration

A single import (`import Bugsy`) exposes the core and the overlay.

```swift
// AppDelegate.application(_:didFinishLaunchingWithOptions:)
Bugsy.shared.initialize(
    config: BugsyConfig(
        trigger: .shake,
        shakeThresholdG: BugsyConfig.companion.DEFAULT_SHAKE_THRESHOLD_G,
        flagsProvider: { [] },
        allowRevealingSecrets: true // false hides the "Copy with real values" button
    )
)
// ...after creating the window:
BugsyIosOverlayKt.startBugsyIosOverlay()
```

- `Bugsy.initialize` must run **before** any `URLSession` is created: it registers Bugsy's `NSURLProtocol`, which intercepts `URLSession.shared` and sessions using the default configuration. Sessions with a custom `URLSessionConfiguration` need to include Bugsy's class in `protocolClasses`.
- In release, use the `:bugsy-noop` framework (not `ui`): it contains no interceptor. As a second barrier, the full framework's interceptor is only created in debug binaries (`Platform.isDebugBinary`).
