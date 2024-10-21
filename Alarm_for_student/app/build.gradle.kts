plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android") // Обновлена версия Kotlin
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
    implementation("androidx.core:core-ktx:1.12.0") // Updated
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2") // Updated
    implementation("androidx.activity:activity-compose:1.8.0") // Updated

    // Coroutine support
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0") // Ensure the latest version
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.0") // Ensure the latest version

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.01.00")) // New BOM version

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")

    implementation("androidx.compose.material3:material3:1.2.0") // Latest stable version of Material3
    implementation("androidx.compose.foundation:foundation:1.5.1") // Latest version Foundation
    implementation("org.jsoup:jsoup:1.16.1") // Latest version Jsoup

    // Navigation Component (if needed)
    implementation("androidx.navigation:navigation-compose:2.5.3") // Latest version

    // ViewModel support
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2") // Latest version

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Compose test support
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.01.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
