# Bugsy Desktop version

_[Também disponível em português](versao-do-desktop.md)._

The Bugsy Desktop version comes from the library's version, so there is one place to change (DEB-70): `VERSION_NAME` in
`gradle.properties`. Releasing a library version already leaves the desktop on the right one.

## The rule

The installers can't take the library's text as it is: `jpackage` (DMG and MSI) only takes `MAJOR.MINOR.PATCH`, macOS
rejects a MAJOR of 0, and a suffix like `-alpha` is not a number. So:

1. the suffix (`-alpha`, `-rc1`, ...) is dropped;
2. a MAJOR of 0 becomes 1.

| `VERSION_NAME` | desktop version |
|---|---|
| `0.3.0-alpha` | `1.3.0` |
| `0.4.1` | `1.4.1` |
| `1.0.0` | `1.0.0` |
| `2.1.0-rc1` | `2.1.0` |

While the library is in the 0 series the desktop shows 1 where the library has 0, and from `1.0.0` on the numbers are equal.

Windows installers also cap the parts (MAJOR and MINOR at 255, PATCH at 65535). A `VERSION_NAME` that is not `X.Y.Z` or
`X.Y.Z-suffix`, or is past those limits, fails the build with a message that names `VERSION_NAME`.

## Where the version shows up

- **Installers and the `.app`:** the derived version (for example `1.3.0`).
- **The app's status bar:** both, `v1.3.0 · Bugsy 0.3.0-alpha`, so you can see which library version the desktop follows.

The app reads both from `bugsy-version.properties`, written at build time from `VERSION_NAME`. Run outside Gradle it shows
`dev` and `unknown` rather than an invented number.

## Where the code is

The rule is in `buildSrc/src/main/kotlin/DesktopVersion.kt`, with tests in `buildSrc/src/test/kotlin/DesktopVersionTest.kt`
(run in CI with `./gradlew :buildSrc:test`). The desktop build only uses it.
