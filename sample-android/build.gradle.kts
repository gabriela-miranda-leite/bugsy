import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "dev.bugsy.sample"
    compileSdk = 37
    compileSdkMinor = 0

    defaultConfig {
        applicationId = "dev.bugsy.sample"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation("androidx.compose.material3:material3")
    implementation(libs.androidx.activity.compose)
    implementation(libs.okhttp)

    // Full Bugsy in debug; in release, only the no-op artifact (same API, no instrumentation).
    // Pass -PbugsyFromMavenLocal=<version> to consume the real published artifacts from
    // mavenLocal instead of the source modules — used by the pre-release gate to test what
    // will actually ship, not just that the source compiles.
    val mavenLocalVersion = findProperty("bugsyFromMavenLocal") as String?
    if (mavenLocalVersion != null) {
        val group = rootProject.property("GROUP") as String
        debugImplementation("$group:bugsy-ui:$mavenLocalVersion")
        releaseImplementation("$group:bugsy-noop:$mavenLocalVersion")
    } else {
        debugImplementation(project(":ui"))
        releaseImplementation(project(":bugsy-noop"))
    }
}
