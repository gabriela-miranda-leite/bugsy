# Bugsy — sample-android

[🇧🇷 Português](README.md) · 🇺🇸 English

Android sample app showing integration with two artifacts:

```kotlin
debugImplementation(project(":ui"))            // full Bugsy
releaseImplementation(project(":bugsy-noop"))  // same API, no instrumentation
```

The app code only uses the public API (`Bugsy.initialize`, `Bugsy.show`, `BugsyAndroidOverlay()`, `OkHttpClient.Builder.addBugsyInterceptor()`), which exists in both artifacts, so the same code compiles in both variants. In **release** the app depends only on `:bugsy-noop`: no capture, repository, or overlay class ends up in the APK, with no reliance on R8/ProGuard rules (which is why there's no `consumer-rules.pro`).

## Checking the release build

```sh
./gradlew :sample-android:assembleRelease :sample-android:assembleDebug
# instrumentation classes in the dex (expected: 0 in release)
unzip -o sample-android/build/outputs/apk/release/sample-android-release-unsigned.apk 'classes*.dex' -d /tmp/rel
cat /tmp/rel/classes*.dex | grep -a -c DebugRepository
# what's left of Bugsy after R8
grep '^com.bugsy' sample-android/build/outputs/mapping/release/mapping.txt
# release variant dependencies
./gradlew :sample-android:dependencies --configuration releaseRuntimeClasspath | grep -i bugsy
```

## Opening Bugsy in debug

The sample uses `Trigger.Notification`: while the app is open, a persistent notification ("Tap to open the debug panel") appears and opens the overlay; it disappears when the app goes to background. On Android 13+ the app needs the `POST_NOTIFICATIONS` permission (the sample requests it on launch). The other triggers are `Trigger.Shake` (default) and `Trigger.FloatingButton`. The **Open Bugsy** button calls `Bugsy.show()` directly.

## Testing a pre-release version

Instead of depending on the local modules (`project(":ui")` / `project(":bugsy-noop")`), the sample can consume artifacts already published to `mavenLocal` — useful for validating the real `.aar` (with R8/shrink in release) before an official release. This is the same mechanism used by the `pre-release.yml` workflow.

```sh
./gradlew :core:publishToMavenLocal :ui:publishToMavenLocal :bugsy-noop:publishToMavenLocal \
  -PVERSION_NAME="0.x.x-rc.1" -PbugsySkipSigning=true

./gradlew :sample-android:assembleDebug -PbugsyFromMavenLocal="0.x.x-rc.1"
```

Without the `-PbugsyFromMavenLocal` flag, the build falls back to the local modules via `project()`.

## Capturing requests

Add the interceptor to the app's client: `OkHttpClient.Builder().addBugsyInterceptor().build()`. In release, the `bugsy-noop` artifact returns the builder unchanged.

## Running on the emulator

```sh
~/Library/Android/sdk/emulator/emulator -avd Pixel_9      # leave this terminal open
./gradlew :sample-android:installDebug                    # in another terminal
```
