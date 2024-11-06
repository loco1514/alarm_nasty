plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services") // Плагин Google Services
}

android {
    namespace = "com.example.alarm_for_student"
    compileSdk = 34 // Обновлено до последней версии SDK

    defaultConfig {
        applicationId = "com.example.alarm_for_student"
        minSdk = 24
        targetSdk = 34 // Обновлено до последней версии SDK
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
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
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.3" // Последняя версия Kotlin Compiler Extension
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core libraries
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.0")

    // Coroutine support
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.01.00"))

    // Core Compose dependencies
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.compose.foundation:foundation:1.5.1")
    implementation("androidx.compose.material:material-icons-extended:1.5.0")

    // Firebase Authentication
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx:21.0.0")
    implementation("com.google.android.gms:play-services-auth:20.3.0") // Для Google Sign-In
    // Import the BoM for the Firebase platform
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))

    // Add the dependency for the Firebase Authentication library
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation("com.google.firebase:firebase-auth")

    // Also add the dependency for the Google Play services library and specify its version
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Jsoup for HTML parsing
    implementation("org.jsoup:jsoup:1.16.1")

    // Navigation Component (if needed)
    implementation("androidx.navigation:navigation-compose:2.6.0-alpha01")

    // DataStore and Gson
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("com.google.code.gson:gson:2.8.8")

    // ViewModel support
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    debugImplementation("androidx.compose.ui:ui-tooling:1.7.3")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.3")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Compose test support
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.01.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
