import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.google.services)
}

// Valeurs propres à chaque étudiant, lues dans local.properties (jamais commité) :
//   PWA_HOST=ma-pwa.netlify.app        → hôte de sa PWA du Jour 3 (App Link, TD A3)
//   SYNC_BASE_URL=https://httpbin.org/ → cible de l'outbox (TD A5)
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val pwaHost: String = localProps.getProperty("PWA_HOST") ?: "mini-podcast.netlify.app"
val syncBaseUrl: String = localProps.getProperty("SYNC_BASE_URL") ?: "https://httpbin.org/"

android {
    namespace = "corsica.universita.voxwave"
    compileSdk = 37

    defaultConfig {
        applicationId = "corsica.universita.voxwave"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1"

        buildConfigField("String", "PWA_HOST", "\"$pwaHost\"")
        buildConfigField("String", "SYNC_BASE_URL", "\"$syncBaseUrl\"")
        manifestPlaceholders["pwaHost"] = pwaHost
    }

    // Keystore debug du cours : tous les étudiants signent avec la même clé,
    // donc une seule empreinte SHA-256 à déclarer (assetlinks.json, Firebase).
    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug { signingConfig = signingConfigs.getByName("debug") }
        release {
            isMinifyEnabled = false // R8 : Jour 6
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

room { schemaDirectory("$projectDir/schemas") }

// Toutes les dépendances des TD A1 à A7 sont déjà déclarées : le premier sync Gradle
// télécharge tout, une fois, avant le 2 novembre.
dependencies {
    // Compose + Material 3
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    // Activity, Lifecycle, Navigation 3
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.navigation3)
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)

    // Core, splash
    implementation(libs.core.ktx)
    implementation(libs.core.splashscreen)

    // Room (TD A4), WorkManager (TD A5), DataStore
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.work.runtime)
    implementation(libs.datastore.preferences)

    // Réseau (TD A5)
    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.serialization.json)

    // Firebase Cloud Messaging & Analytics (Annexe 4)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)

    // CameraX et localisation (TD A7)
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.camerax.compose)
    implementation(libs.play.services.location)

    testImplementation(libs.junit)
}
