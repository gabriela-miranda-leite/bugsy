// Gradle's embedded Kotlin pins org.jetbrains:annotations strictly to 13.0 on the buildscript
// classpath, but AGP 9.5.1's own transitive deps (ddmlib, layoutlib-api, repository) need 23.0.0.
// Force the newer, backward-compatible version to resolve the conflict.
buildscript {
    configurations.classpath {
        resolutionStrategy.force("org.jetbrains:annotations:23.0.0")
    }
}

plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.buildkonfig) apply false
    alias(libs.plugins.mavenPublish) apply false
    alias(libs.plugins.kover)
}

// Merged coverage report (core + ui) measured from their Android/JVM unit test runs — Kover doesn't
// instrument the iOS targets, so native code isn't part of this number.
dependencies {
    kover(project(":core"))
    kover(project(":ui"))
}

// group/version must be set on the subprojects BEFORE the publish plugin is applied
// (com.vanniktech.maven.publish reads project.group/project.version when the plugin is
// applied) — that's why they live here, evaluated before each module's build.gradle.kts runs.
subprojects {
    group = rootProject.property("GROUP") as String
    version = rootProject.property("VERSION_NAME") as String
}
