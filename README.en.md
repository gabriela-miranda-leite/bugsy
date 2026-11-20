<p align="center">
  <img src="assets/logo.svg" alt="Bugsy" width="360">
</p>

<p align="center">
<a href="https://central.sonatype.com/namespace/io.github.gabriela-miranda-leite"><img alt="Maven Central" src="https://img.shields.io/maven-central/v/io.github.gabriela-miranda-leite/bugsy-ui"></a>
<a href="https://github.com/gabriela-miranda-leite/bugsy/actions/workflows/android.yml"><img alt="Android" src="https://github.com/gabriela-miranda-leite/bugsy/actions/workflows/android.yml/badge.svg"></a>
<a href="https://github.com/gabriela-miranda-leite/bugsy/actions/workflows/ios.yml"><img alt="iOS" src="https://github.com/gabriela-miranda-leite/bugsy/actions/workflows/ios.yml/badge.svg"></a>
<a href="https://codecov.io/gh/gabriela-miranda-leite/bugsy"><img alt="codecov" src="https://codecov.io/gh/gabriela-miranda-leite/bugsy/graph/badge.svg"></a>
<a href="https://github.com/gabriela-miranda-leite/bugsy/blob/main/scripts/verify-signature.md"><img alt="GPG signed" src="https://img.shields.io/badge/artifacts-GPG%20signed-0A7BBB"></a>
<a href="LICENSE"><img alt="License: MIT" src="https://img.shields.io/badge/license-MIT-blue.svg"></a>
</p>

<p align="center">
<a href="README.md">🇧🇷 Português</a> · 🇺🇸 English
</p>

Bugsy is a **Kotlin Multiplatform (Android + iOS)** library that works like a pocket-sized
"Flipper/Chucker" embedded right into the app: a debug overlay that captures network requests,
lets you create mock rules to simulate responses (success, error, slow network) without touching
the backend, shows feature flags and device info, and lets you reveal/mask sensitive data, with a
"when in doubt, mask it" security policy so secrets (tokens, passwords, national IDs) never leak
by accident.

## Table of contents

- [Demo](#demo)
- [Installation](#installation)
  - [Kotlin / Android](#kotlin--android)
  - [Swift / iOS](#swift--ios)
  - [React Native](#react-native)
  - [Flutter](#flutter)
- [Basic usage](#basic-usage)
- [Mocks](#mocks)
- [Bugsy Desktop](#bugsy-desktop)
- [Modules](#modules)
- [Security](#security)
- [About development](#about-development)

## Demo

| Android | iOS |
|---|---|
| <video src="https://github.com/user-attachments/assets/d0d36d73-6a73-4659-8ef9-784bb9906590" controls width="300"></video> | <video src="https://github.com/user-attachments/assets/e4013b78-c22d-4890-91e5-82c7855eb68d" controls width="300"></video> |

## Installation

Published on **Maven Central** under the group `io.github.gabriela-miranda-leite`.

### Kotlin / Android

3 artifacts consumed separately by build type:

```kotlin
dependencies {
    // Full overlay (pulls in :core transitively), debug only.
    debugImplementation("io.github.gabriela-miranda-leite:bugsy-ui:0.1.0-alpha")

    // Same public API, zero instrumentation, for release.
    releaseImplementation("io.github.gabriela-miranda-leite:bugsy-noop:0.1.0-alpha")
}
```

### Swift / iOS

Bugsy also runs in **pure Swift** apps (no KMP needed), via `Bugsy.xcframework`:

```sh
# Debug: full Bugsy
./gradlew :ui:linkDebugFrameworkIosSimulatorArm64 :ui:iosSimulatorArm64AggregateResources
# Release: no-op artifact (same API, no instrumentation)
./gradlew :bugsy-noop:linkReleaseFrameworkIosSimulatorArm64
```

The resources task (`...AggregateResources`) generates the overlay's strings and fonts and only
exists on the Debug target — without it the app may use stale resources and crash when opening the
overlay. To run on a physical device, swap `iosSimulatorArm64` for `iosArm64`.

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

For the app to react right away when a flag changes (in the overlay or on Bugsy Desktop), observe the
`BugsyIosOverlayKt.BUGSY_FLAGS_DID_CHANGE_NOTIFICATION` notification and read the flag again with
`Bugsy.shared.flagManager.value(key:)`; the `userInfo` carries the `key`. It starts with `startBugsyIosOverlay()`.

`Bugsy.initialize` needs to run **before** any `URLSession` is created (it registers Bugsy's
`NSURLProtocol`). In release, link the `:bugsy-noop` framework, not `:ui` — it doesn't contain the
interceptor. See [`sample-ios/README.md`](sample-ios/README.md) for the full step-by-step,
including copying the overlay resources in Xcode.

### React Native

Bugsy is a native library (Kotlin/Swift) and **has no JS binding today** — you can't `import` it
directly into a React Native app. Using it in RN would require writing a native module (Turbo
Module/Native Module) wrapping the `Bugsy.*` calls on the Android and iOS side. That wrapper
doesn't exist yet; it's future work.

### Flutter

Same situation as React Native: **no Flutter plugin today**. Integrating it would require a
Platform Channel (or Federated Plugin) calling native Bugsy on each platform. That plugin doesn't
exist yet; it's future work.

## Basic usage

```kotlin
// Application.onCreate()
Bugsy.initialize(
    trigger = Trigger.Shake,
    flagsProvider = { listOf(DebugFlag("darkModeEnabled", "Dark mode", false)) },
)

// OkHttp client (Android)
OkHttpClient.Builder()
    .addBugsyInterceptor()
    .build()
```

On iOS, call `startBugsyIosOverlay()` once at app startup to observe `Bugsy.isVisible`.

## Mocks

The overlay's "Mocks" tab lets you create rules that intercept requests by method and URL
pattern (regex) and respond with a custom status, headers and body, with no need to touch or
redeploy the backend. Each rule can have a simulated delay, be toggled off temporarily, or set to
fire only once ("one shot"). You can also pre-fill a rule from a real captured request ("Create
mock from this response"), and export/import the whole rule set as a JSON file to share between
devices or with the team.

| Android | iOS |
|---|---|
| <video src="https://github.com/user-attachments/assets/0beb8daa-9e65-485a-a59d-dea6fae9b89f" controls width="300"></video> | <video src="https://github.com/user-attachments/assets/08e92c81-3fd4-4ddc-a94e-95e9a4b42c5d" controls width="300"></video> |

## Bugsy Desktop

Desktop client (macOS, Windows, Linux) that pairs with the phone over Wi-Fi to view requests,
mocks, flags and device info on a bigger screen. Download the installer for your system from the
[Releases page](https://github.com/gabriela-miranda-leite/bugsy/releases/latest):

- **macOS**: `Bugsy-<version>.dmg`
- **Windows**: `Bugsy-<version>.msi`
- **Linux**: `bugsy_<version>-1_amd64.deb`

The app isn't signed or notarized yet — that requires a paid developer certificate (an Apple
Developer Program membership and a Windows code signing certificate), which is out of scope for
now. Without them, the OS shows an "untrusted app" warning on first launch; the workaround is:

- **macOS**: right-click (or Control-click) `Bugsy.app` and choose "Open", confirming the
  Gatekeeper dialog. It opens normally after that.
- **Windows**: on the SmartScreen warning, click "More info" and then "Run anyway".
- **Linux**: install with `sudo dpkg -i bugsy_<version>-1_amd64.deb` (or your package manager's
  GUI); with no Gatekeeper/SmartScreen, it opens right away.

To run from source during development: `./gradlew :bugsy-desktop:run`.

## Modules

```
settings.gradle.kts
 ├─ :core            (KMP lib — capture, redaction, logic)
 ├─ :ui              (KMP lib — Compose Multiplatform overlay, depends on :core)
 ├─ :bugsy-noop      (KMP lib — same public API as core+ui, zero instrumentation)
 └─ :sample-android  (Android demo app, not published)
```

- **`core`** — request capture (OkHttp interceptor on Android, `NSURLProtocol` on iOS), header
  redaction and sensitive body field masking, feature flags, and the triggers (shake, floating
  button, notification) that open the overlay.
- **`ui`** — Compose Multiplatform overlay: request list, request/response detail, mock rules
  (creation, editing, JSON import/export), flags and device info.
- **`bugsy-noop`** — artifact for release builds: same public API as `core`+`ui`, with no
  instrumentation at all.

## Security

Sensitive headers (`authorization`, `cookie`, `x-api-key`, ...) and body fields (`password`,
national ID fields, `token`, `secret`, ...) are masked by default before any data reaches the UI or
the exported `cURL`. Real data only ever lives in memory, never in logs, and only becomes available
if explicitly revealed in the overlay's settings screen.

In release, `bugsy-noop` replaces `ui`/`core` with an artifact that has no behavior at all — the
actual gate, though, is that the interceptor/`NSURLProtocol` is only activated if the host is
debuggable, even when the full artifact is on the classpath.

The connection between the phone (Android and iOS) and Bugsy Desktop uses TLS with a pinned
certificate, and pairing encrypts the token. When pairing by code or from the device list, you
compare a 4-digit code on both screens. What is and is not protected is described in
[`docs/seguranca-do-pareamento-desktop.en.md`](docs/seguranca-do-pareamento-desktop.en.md)
([versão em português](docs/seguranca-do-pareamento-desktop.md)).

## About development

This project was developed with AI assistance ([Claude Code](https://claude.com/claude-code)),
with human review on every PR.
