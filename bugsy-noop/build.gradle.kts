import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.mavenPublish)
    id("signing")
}

mavenPublishing {
    coordinates(artifactId = "bugsy-noop")
    publishToMavenCentral()
    signAllPublications()
    configure(KotlinMultiplatform(javadocJar = JavadocJar.Empty(), androidVariantsToPublish = listOf("release")))
    pom {
        name.set("Bugsy Noop")
        description.set("Bugsy's no-op artifact: same public API as core+ui, with no instrumentation, for release builds.")
        url.set("https://github.com/gabriela-miranda-leite/bugsy")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://github.com/gabriela-miranda-leite/bugsy/blob/main/LICENSE")
            }
        }
        developers {
            developer {
                id.set("gabriela-miranda-leite")
                name.set("Gabriela Miranda")
                url.set("https://github.com/gabriela-miranda-leite")
            }
        }
        scm {
            url.set("https://github.com/gabriela-miranda-leite/bugsy")
            connection.set("scm:git:https://github.com/gabriela-miranda-leite/bugsy.git")
            developerConnection.set("scm:git:ssh://git@github.com/gabriela-miranda-leite/bugsy.git")
        }
    }
}

signing {
    useGpgCmd()
}

// Artifact for release builds: same public API as core + ui, with no instrumentation.
// Usage: debugImplementation(bugsy) + releaseImplementation(bugsy-noop).
kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    val xcframework = XCFramework("Bugsy")

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { target ->
        target.binaries.framework {
            baseName = "Bugsy"
            isStatic = true
            xcframework.add(this)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            compileOnly(libs.okhttp)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "com.bugsy.noop"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
