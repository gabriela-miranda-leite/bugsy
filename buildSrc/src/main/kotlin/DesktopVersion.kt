/**
 * The version of the packaged Bugsy Desktop, derived from the library's `VERSION_NAME` so there is one place to bump (DEB-70).
 *
 * The installers can't carry the library's text as it is: `jpackage` (DMG and MSI) only takes `MAJOR.MINOR.PATCH`, macOS
 * rejects a MAJOR of 0, and a suffix like `-alpha` is not a number. So the suffix is dropped and a leading 0 becomes 1:
 * `0.3.0-alpha` is `1.3.0`, `0.4.1` is `1.4.1`, and from `1.0.0` on the two are the same number. The library version the
 * desktop follows is still shown in the app, so the mapping is never a guess.
 *
 * Windows installers also cap the parts (MAJOR and MINOR at 255, PATCH at 65535), so a version past that fails here with a
 * clear message instead of deep inside the packaging.
 */
fun desktopPackageVersion(libraryVersion: String): String {
    val text = libraryVersion.trim()
    val match = Regex("""^(\d+)\.(\d+)\.(\d+)(?:-[0-9A-Za-z.]+)?$""").matchEntire(text)
        ?: throw IllegalArgumentException(
            "VERSION_NAME '$libraryVersion' is not X.Y.Z or X.Y.Z-suffix (for example 0.3.0-alpha), so the desktop package version can't be derived from it.",
        )
    val (rawMajor, rawMinor, rawPatch) = match.destructured
    val major = rawMajor.toLongOrNull()?.let { if (it == 0L) 1L else it }
    val minor = rawMinor.toLongOrNull()
    val patch = rawPatch.toLongOrNull()
    require(major != null && major <= 255) { "VERSION_NAME '$libraryVersion': the major part must be at most 255 for the installers." }
    require(minor != null && minor <= 255) { "VERSION_NAME '$libraryVersion': the minor part must be at most 255 for the installers." }
    require(patch != null && patch <= 65535) { "VERSION_NAME '$libraryVersion': the patch part must be at most 65535 for the installers." }
    return "$major.$minor.$patch"
}
