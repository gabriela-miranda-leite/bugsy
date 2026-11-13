pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "bugsy"

include(":core")
include(":ui")
include(":bugsy-noop")
include(":sample-android")
