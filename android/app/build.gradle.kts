import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Release signing credentials. The keystore and its passwords must never be
// committed: put them in android/keystore.properties (gitignored - copy
// keystore.properties.example), or supply them as environment variables for
// CI. With neither present the release build is simply left unsigned, so
// `assembleRelease` still works for anyone without the key.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

fun signingSetting(propertyName: String, envName: String): String? =
    (keystoreProperties.getProperty(propertyName) ?: System.getenv(envName))
        ?.takeIf { it.isNotBlank() }

val releaseStoreFile = signingSetting("storeFile", "FMP_STORE_FILE")
val releaseStorePassword = signingSetting("storePassword", "FMP_STORE_PASSWORD")
val releaseKeyAlias = signingSetting("keyAlias", "FMP_KEY_ALIAS")
val releaseKeyPassword = signingSetting("keyPassword", "FMP_KEY_PASSWORD")
val hasReleaseSigning = releaseStoreFile != null && releaseStorePassword != null &&
    releaseKeyAlias != null && releaseKeyPassword != null

android {
    namespace = "com.pebblephonefinder.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pebblephonefinder.android"
        minSdk = 26
        targetSdk = 36
        // versionName restarted at 1.0.0 for the first real release, after
        // the 0.x.y-debug series. versionCode must NOT restart with it - it
        // only ever counts upward, or Play rejects the upload. It carried on
        // from where the debug series left off. See release/README.md.
        versionCode = 17
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            // Resource shrinking stays off: this app is small, and it strips
            // resources it can't see referenced, which fails at runtime
            // rather than at build time. Not worth the risk for the saving.
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                logger.lifecycle(
                    "No release signing credentials found - the release build will be unsigned. " +
                        "See android/keystore.properties.example."
                )
            }
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
        buildConfig = true
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
