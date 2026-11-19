import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.paparazzi)
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.kover)
    id("signing")
}

// Fixed so it doesn't depend on the project's `group` (which publish changes from "bugsy" to the real GROUP) —
// the generated Res class package is referenced throughout commonMain and must not change between builds.
compose.resources {
    packageOfResClass = "bugsy.ui.generated.resources"
}

mavenPublishing {
    coordinates(artifactId = "bugsy-ui")
    publishToMavenCentral()
    if (!project.hasProperty("bugsySkipSigning")) {
        signAllPublications()
    }
    configure(KotlinMultiplatform(javadocJar = JavadocJar.Empty(), androidVariantsToPublish = listOf("release")))
    pom {
        name.set("Bugsy UI")
        description.set("Bugsy's Compose Multiplatform overlay — request list, flags and device info.")
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
    if (!project.hasProperty("bugsySkipSigning")) {
        if (System.getenv("CI") != null) {
            useInMemoryPgpKeys(System.getenv("SIGNING_KEY"), System.getenv("SIGNING_PASSWORD"))
        } else {
            useGpgCmd()
        }
    }
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    // The "Bugsy" framework bundles core (exported) and the overlay, so a pure Swift app can use everything with a single import.
    val xcframework = XCFramework("Bugsy")

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { target ->
        target.binaries.framework {
            baseName = "Bugsy"
            isStatic = true
            export(project(":core"))
            xcframework.add(this)
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core)
            implementation(libs.androidx.activity.compose)
            // Camera preview + QR decoding for "Scan QR code on desktop" (DEB-64).
            implementation(libs.androidx.camera.core)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)
            implementation(libs.zxing.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        androidUnitTest.dependencies {
            implementation(kotlin("test"))
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
            implementation(libs.androidx.compose.ui.test.junit4)
            implementation(libs.androidx.compose.ui.test.manifest)
            implementation(libs.robolectric)
        }
    }
}

android {
    namespace = "com.bugsy.ui"
    compileSdk = 37
    compileSdkMinor = 0

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

androidComponents {
    beforeVariants {
        (it as? com.android.build.api.variant.HasUnitTestBuilder)?.enableUnitTest = true
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

tasks.withType<Test>().configureEach {
    // compose-resources resolves the language via android.os.LocaleList.getDefault(), which android-all
    // pins from the JVM locale at startup — without this, each runner inherits the host locale
    // (pt-BR on a dev Mac, en on CI) and text snapshots vary between them.
    systemProperty("user.language", "pt")
    systemProperty("user.country", "BR")
}
