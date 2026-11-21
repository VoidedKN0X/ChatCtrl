plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.vknxworks.chatctrl"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.vknxworks.chatctrl"
        minSdk = 35
        targetSdk = 36
        // Bump both to publish a new release (see CHANGELOG.md).
        versionCode = 72
        versionName = "2.2.0"

        // The Rust SDK ships a native library per CPU type (~60 MB each). Phones running Android 15+
        // are 64-bit ARM, so only that one is packaged; add "x86_64" here to run on an emulator.
        ndk { abiFilters += listOf("arm64-v8a") }
    }

    // Release signing. On GitHub Actions the build job has no signing key at all: it sets UNSIGNED_RELEASE=true
    // and gets app-release-unsigned.apk, which a separate job signs with apksigner (see .github/workflows/build.yml).
    // An unsigned APK cannot be installed, so nothing unsigned can ship. A keystore from environment variables still
    // works (SIGNING_KEYSTORE_PATH, SIGNING_STORE_PASSWORD, SIGNING_KEY_ALIAS, SIGNING_KEY_PASSWORD). Local builds
    // without one fall back to the debug key; on CI a build that has neither a key nor UNSIGNED_RELEASE=true fails.
    val ksPath = System.getenv("SIGNING_KEYSTORE_PATH")
    val onCi = System.getenv("CI") == "true" || System.getenv("GITHUB_ACTIONS") == "true"
    val signingReady = !ksPath.isNullOrBlank() && file(ksPath).exists() &&
        !System.getenv("SIGNING_STORE_PASSWORD").isNullOrBlank() &&
        !System.getenv("SIGNING_KEY_ALIAS").isNullOrBlank() &&
        !System.getenv("SIGNING_KEY_PASSWORD").isNullOrBlank()
    val unsignedRelease = System.getenv("UNSIGNED_RELEASE") == "true"
    if (onCi && !signingReady && !unsignedRelease) {
        throw GradleException("Release signing values are missing on CI and UNSIGNED_RELEASE=true is not set; refusing to sign the release with the debug key.")
    }
    signingConfigs {
        if (signingReady) {
            create("release") {
                storeFile = file(ksPath!!)
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Asked for explicitly: no signing here at all, the output is app-release-unsigned.apk.
            signingConfig = if (unsignedRelease) null else (
                signingConfigs.findByName("release")
                    ?: if (onCi) throw GradleException("No release signing config on CI") else signingConfigs.getByName("debug")
                )
        }
    }

    packaging {
        // Compress the big native SDK library inside the APK so the download stays smaller.
        jniLibs { useLegacyPackaging = true }
        resources { excludes += setOf("META-INF/*.kotlin_module", "META-INF/AL2.0", "META-INF/LGPL2.1") }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    androidResources {
        // Never ship source maps (element-call/build.sh doesn't make them, this is a second guard).
        ignoreAssetsPattern = "!.svn:!.git:!.ds_store:!*.scc:.*:<dir>_*:!CVS:!thumbs.db:!picasa.ini:!*~:!*.map"
    }
    sourceSets {
        // Element Call, built from source by element-call/build.sh, is served from the APK's assets as element-call/.
        getByName("main").assets.srcDir(rootProject.file("element-call/dist"))
    }
    // No dependency metadata blob in the APK: it's encrypted for Google Play, so nobody checking the build can read it.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.foundation.ExperimentalFoundationApi",
            "androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "androidx.compose.ui.text.ExperimentalTextApi",
        )
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-process:2.10.0")
    implementation("androidx.navigation:navigation-compose:2.9.8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("androidx.browser:browser:1.8.0")
    implementation("androidx.work:work-runtime-ktx:2.10.1")

    // Matrix: the same Rust SDK bindings Element X uses (sync, timeline, end-to-end encryption, verification).
    implementation("org.matrix.rustcomponents:sdk-android:26.10.15")
    implementation("net.java.dev.jna:jna:5.18.1@aar")
    implementation("org.rustls:rustls-platform-verifier:0.2.0@aar")

    // Frosted-glass blur behind the floating composer island, as in the other Ctrl apps.
    implementation("dev.chrisbanes.haze:haze:1.6.0")

    val coil = "3.1.0"
    implementation("io.coil-kt.coil3:coil-compose:$coil")
    // Plays animated GIFs and stickers other people send.
    implementation("io.coil-kt.coil3:coil-gif:$coil")

    // Sign in and link devices with a QR code: the camera (CameraX) and the open-source zxing-cpp decoder read the
    // code on the phone, and Nayuki's small QR Code generator draws ours. Nothing is sent anywhere to read or make them.
    val camerax = "1.4.2"
    implementation("androidx.camera:camera-camera2:$camerax")
    implementation("androidx.camera:camera-lifecycle:$camerax")
    implementation("androidx.camera:camera-view:$camerax")
    implementation("io.github.zxing-cpp:android:2.3.0")
    implementation("io.nayuki:qrcodegen:1.8.0")

    // Sending and viewing locations on an OpenStreetMap map (no Google Maps).
    implementation("org.osmdroid:osmdroid-android:6.1.20")
}

// Voice and video calls need Element Call, built from source (Element Call 0.28.0, the release Element X bundles,
// without its Google, analytics and crash-reporting code) by element-call/build.sh into element-call/dist/. It's
// loaded from the APK into the system WebView and driven through the Rust SDK's widget API; nothing is loaded from
// the web. A build without it would have no call screen, so it stops here instead.
val elementCallIndex = rootProject.file("element-call/dist/element-call/index.html")
tasks.named("preBuild") {
    doFirst {
        if (!elementCallIndex.isFile) {
            throw GradleException("Element Call isn't built: run element-call/build.sh first (needs Node.js 24 and Corepack 0.34.5 or newer).")
        }
    }
}
