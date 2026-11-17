pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        // Only consulted when the sample is told to consume the published artifacts
        // (-PbugsyFromMavenLocal=<version>, used by the pre-release gate).
        if (providers.gradleProperty("bugsyFromMavenLocal").isPresent) {
            mavenLocal()
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "bugsy"

include(":core")
include(":ui")
include(":bugsy-noop")
include(":sample-android")
include(":bugsy-desktop")
