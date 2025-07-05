// Top-Level Plugins Block
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt") // Für Hilt
    id("com.google.dagger.hilt.android") // Hilt Plugin
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0" // Kotlinx Serialization
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.quantum_prof.phantalandwaittimes" // DEIN PAKETNAME
    compileSdk = 35 // Oder neueste SDK

    defaultConfig {
        applicationId = "com.quantum_prof.phantalandwaittimes" // DEIN PAKETNAME
        minSdk = 26
        targetSdk = 34
        versionCode = 4
        versionName = "2.0.0" // Version anpassen

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    //*composeOptions {
    //     kotlinCompilerExtensionVersion = "1.5.3" // Kompatible Version prüfen!
    //}
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core & AppCompat
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.0") // Kann ggf. weg bei reinen Compose-Apps

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2025.04.00")) // Neueste BOM prüfen
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3") // Material 3
    implementation("androidx.activity:activity-compose:1.10.1")

    // Retrofit & Kotlinx Serialization
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0") // Für Debugging

    // Hilt (Dependency Injection)
    implementation("com.google.dagger:hilt-android:2.48.1") // Neueste Version prüfen
    kapt("com.google.dagger:hilt-compiler:2.48.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0") // Neueste Version prüfen

    // --- NEU: WorkManager für Hintergrund-Benachrichtigungen ---
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.1.0")
    kapt("androidx.hilt:hilt-compiler:1.1.0")

    // Accompanist SwipeRefresh (Pull-to-Refresh)
    implementation("com.google.accompanist:accompanist-swiperefresh:0.32.0") // Neueste Version prüfen

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.04.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    implementation("androidx.compose.material3:material3:1.3.2") // Überprüfe die neueste Version!

    // Oft wird auch die Compose BOM verwendet, die Versionen verwaltet:
    implementation(platform("androidx.compose:compose-bom:2025.04.00"))
    implementation("com.google.android.material:material:1.12.0")

    implementation(platform("androidx.compose:compose-bom:2025.04.00")) // Stelle sicher, dass die BOM aktuell ist
    // Icons Core (enthält die grundlegenden Icons)
    implementation("androidx.compose.material:material-icons-core")
    // Icons Extended (enthält viele weitere Icons, inkl. Sort) - Sicher ist sicher
    implementation("androidx.compose.material:material-icons-extended")
    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Hilt WorkManager Integration
    implementation("androidx.hilt:hilt-work:1.1.0")
    kapt("androidx.hilt:hilt-compiler:1.1.0")
}

// Hilt Kapt Konfiguration
kapt {
    correctErrorTypes = true
}