plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.code"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.code"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // The connected-test report counts failed assumptions as failures on this runner.
        // Keep the live Firestore tests out of discovery unless their flags are enabled.
        val readOwnerReservations =
            providers.gradleProperty("android.testInstrumentationRunnerArguments.readOwnerReservations")
        if (readOwnerReservations.orNull != "true") {
            testInstrumentationRunnerArguments["notClass"] =
                "com.example.code.CampsiteOwnerMessagesTest#liveOwnerInboxesReflectConfirmedReservations"
        }
        val writeReviewerFirestore =
            providers.gradleProperty("android.testInstrumentationRunnerArguments.writeReviewerFirestore")
        if (writeReviewerFirestore.orNull != "true") {
            testInstrumentationRunnerArguments["notAnnotation"] = "com.example.code.ReviewerFirestoreWrite"
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    packaging {
        // MockK's dependencies ship duplicate licence files that break the androidTest build
        resources {
            excludes += setOf("META-INF/LICENSE.md", "META-INF/LICENSE-notice.md")
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    //implementation(libs.androidx.ui)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation("io.mockk:mockk-android:1.14.9")
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation("androidx.compose.material:material-icons-core")
    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-analytics")
    // Reads QR codes from images (entrant QR scanning in EntrantProfileScreen.kt)
    implementation("com.google.zxing:core:3.5.3")
}
