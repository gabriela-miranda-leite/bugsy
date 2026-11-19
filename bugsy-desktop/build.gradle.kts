import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    jvmToolchain(17)
}

// The mobile overlay's Roboto and Roboto Mono, read from where `:ui` keeps them, so there is one copy of each font and
// the desktop can't drift from the mobile typography (DEB-68). They land at the root of the classpath.
sourceSets {
    main {
        resources.srcDir("../ui/src/commonMain/composeResources/font")
        // The Bugsy mark, from the one file every platform's logo is made from (see `generateBrandAssets`). The desktop
        // draws this very file, so there is no copy of it here.
        resources.srcDir("../assets/brand")
    }
}

// Regenerates every platform's logo and app icons from assets/brand/bugsy-mark.svg (DEB-68).
tasks.register<JavaExec>("generateBrandAssets") {
    group = "brand"
    description = "Generates the vector drawables and app icons of every platform from assets/brand/bugsy-mark.svg."
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.bugsy.desktop.brand.BrandAssetsKt")
    args(rootProject.projectDir.path)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    // OkHttp, not Ktor's own CIO engine: CIO implements TLS itself, and under load it read garbage ("Invalid TLS record
    // type code") or never finished a close, against a plain JDK TLS server. OkHttp uses the JDK's TLS.
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.websockets)
    implementation(libs.zxing.core)

    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.core)
    testImplementation(libs.ktor.server.cio)
    testImplementation(libs.ktor.server.websockets)
    testImplementation(libs.ktor.network.tls.certificates)
}

// The version of the packaged app is derived from the library's, so there is one place to bump (DEB-70). The rule is in
// buildSrc/src/main/kotlin/DesktopVersion.kt, with its tests.
val libraryVersion = project.property("VERSION_NAME") as String
val desktopVersion: String = try {
    desktopPackageVersion(libraryVersion)
} catch (error: IllegalArgumentException) {
    throw GradleException(error.message ?: "Can't derive the desktop package version from VERSION_NAME.", error)
}

// The app shows both versions (its own and the library's it follows), read from this file at runtime.
val generateVersionProperties = tasks.register("generateVersionProperties") {
    val outputDir = layout.buildDirectory.dir("generated/version")
    inputs.property("library", libraryVersion)
    inputs.property("app", desktopVersion)
    outputs.dir(outputDir)
    doLast {
        outputDir.get().asFile.apply { mkdirs() }.resolve("bugsy-version.properties").writeText("app=$desktopVersion\nlibrary=$libraryVersion\n")
    }
}
sourceSets.main { resources.srcDir(generateVersionProperties) }

compose.desktop {
    application {
        mainClass = "com.bugsy.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            // What a person sees once it is installed: the app name in the Dock, Launchpad and Start menu, and the
            // installer's file name. The window title stays "Bugsy Desktop", which says what it is.
            packageName = "Bugsy"
            packageVersion = desktopVersion
            description = "Remote debug client for apps embedding Bugsy."
            // The app icon of each platform, generated from assets/brand/bugsy-mark.svg (`generateBrandAssets`).
            macOS {
                dockName = "Bugsy"
                iconFile.set(project.file("packaging/icon.icns"))
            }
            windows { iconFile.set(project.file("packaging/icon.ico")) }
            linux { iconFile.set(project.file("packaging/icon.png")) }
        }
    }
}
