@file:Suppress("OPT_IN_USAGE")

import com.codingfeline.buildkonfig.compiler.FieldSpec.Type
import com.codingfeline.buildkonfig.gradle.BuildKonfigExtension
import org.jetbrains.kotlin.konan.properties.Properties


plugins {
    kotlin("native.cocoapods")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxSerialization)
    id("com.android.library")
    id("com.codingfeline.buildkonfig") version "0.15.0"
}

version = "1.0"

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "21"
            }
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    js(IR) {
        browser()
    }

    wasmJs {
        browser()
    }

    cocoapods {
        summary = "Shared code for the sample"
        homepage = "https://github.com/example/base-kmp-ai-agent"
        version = "1.0"
        authors = "Development Team"
        license = "MIT"
        ios.deploymentTarget = "14.1"
        podfile = project.file("../iosApp/Podfile")
        framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        all {
            languageSettings {
                optIn("kotlin.uuid.ExperimentalUuidApi")
            }
        }

        commonMain {
            languageSettings {
                languageVersion = "2.1"
                apiVersion      = "2.1"
            }
            dependencies {
                implementation(compose.ui)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.runtime)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.components.resources)

                //ktor-client
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.json)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.client.serialization)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)

                implementation(libs.coil.compose)
                implementation(libs.coil.network.ktor)

                //compose navigation
                implementation(libs.navigation.compose)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.material)
                implementation(libs.ktor.client.okhttp)
            }
        }

        iosMain {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }


        jsMain {
            dependencies {
                implementation(libs.ktor.client.js)
            }
        }
    }
}


android {
    compileSdk = 34
    namespace = "com.base.shared"
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.0"
    }
}

@Suppress("TooGenericExceptionCaught")
configure<BuildKonfigExtension> {
    packageName = "com.base.shared"
    val properties = Properties()

    val rootProjectDir = project.rootProject.rootDir
    val secretProperties = File(rootProjectDir, "secret.properties")
    if (secretProperties.exists()) {
        properties.load(secretProperties.inputStream())
    }

//    val apiKey = properties.getProperty("WEATHER_API_KEY") ?: System.getenv("WEATHER_API_KEY")

    defaultConfigs {
        buildConfigField(
            Type.STRING,
            "WEATHER_API_KEY",
            "90d78fc11f04c530a4e6f6c01cbb66e5"
        )
    }
}

