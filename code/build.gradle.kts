// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.gms.google-services") version "4.5.0" apply false
}

// Codex prompt: "can it be done automatically in the test code? do that"
tasks.register<Exec>("reservationRulesFirestoreTest") {
    group = "verification"
    description = "Start local Firestore, connect ADB, run the reservation rules integration test, and clean up."
    workingDir = rootDir
    commandLine("node", "scripts/run-reservation-rules-test.js")
}
