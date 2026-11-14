plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.buildkonfig) apply false
    alias(libs.plugins.mavenPublish) apply false
}

// group/version must be set on the subprojects BEFORE the publish plugin is applied
// (com.vanniktech.maven.publish reads project.group/project.version when the plugin is
// applied) — that's why they live here, evaluated before each module's build.gradle.kts runs.
subprojects {
    group = rootProject.property("GROUP") as String
    version = rootProject.property("VERSION_NAME") as String
}
