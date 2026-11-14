import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.paparazzi)
    id("maven-publish")
}

group = project.property("GROUP") as String
version = project.property("VERSION_NAME") as String

// Fixo para não depender do `group` do projeto (que o publish muda de "bugsy" para "com.bugsy") —
// o pacote da classe Res gerada é referenciado em todo o commonMain e não pode mudar entre builds.
compose.resources {
    packageOfResClass = "bugsy.ui.generated.resources"
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    // O framework "Bugsy" inclui o core (exportado) e o overlay, para um app Swift puro usar tudo com um único import.
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
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidUnitTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "com.bugsy.ui"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

tasks.withType<Test>().configureEach {
    // compose-resources resolve o idioma via android.os.LocaleList.getDefault(), que o android-all
    // fixa a partir do locale do JVM na inicialização — sem isso, cada runner herda o locale do
    // host (pt-BR no Mac de desenvolvimento, en no CI) e os snapshots de texto variam entre eles.
    systemProperty("user.language", "pt")
    systemProperty("user.country", "BR")
}
