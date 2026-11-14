import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.kover)
    id("signing")
}

mavenPublishing {
    coordinates(artifactId = "bugsy-core")
    publishToMavenCentral()
    if (!project.hasProperty("bugsySkipSigning")) {
        signAllPublications()
    }
    configure(KotlinMultiplatform(javadocJar = JavadocJar.Empty(), androidVariantsToPublish = listOf("release")))
    pom {
        name.set("Bugsy Core")
        description.set("Bugsy's models, OkHttp/URLProtocol interceptor and redaction logic.")
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

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            api(libs.okhttp)
            implementation(libs.androidx.startup.runtime)
            implementation(libs.androidx.lifecycle.process)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidUnitTest.dependencies {
            implementation(libs.okhttp.mockwebserver)
        }
    }
}

buildkonfig {
    packageName = "com.bugsy.core"

    defaultConfigs {
        buildConfigField(STRING, "VERSION", project.property("VERSION_NAME") as String)
    }
}

android {
    namespace = "com.bugsy.core"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        buildConfig = true
    }
}
