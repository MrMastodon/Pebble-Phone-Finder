plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.pebblephonefinder.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pebblephonefinder.android"
        minSdk = 26
        targetSdk = 36
        // Versioning scheme: debug builds use a 0.x.y series (bump minor on
        // every new debug build we ship, e.g. 0.1.0 -> 0.2.0). The first
        // real release build starts a fresh series at 1.0.0, independent of
        // wherever the debug series was left. See release/README.md.
        versionCode = 2
        versionName = "0.2.0-debug"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        viewBinding = true
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("com.google.android.material:material:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Third-party Android API for talking to PebbleOS watches through the
    // official Pebble companion app. See docs/PROTOCOL.md.
    // Coordinate/version per https://github.com/pebble-dev/PebbleKitAndroid2
    // (JitPack-hosted); bump the version here if a newer release is available.
    implementation("io.rebble.pebblekit2:client:1.2.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.12")
}
