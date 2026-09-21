plugins {
    id("com.android.application") version "9.0.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20"
}

android {
    namespace = "dev.appspike.sample"
    compileSdk = 36
    defaultConfig {
        applicationId = "dev.appspike.sample"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

dependencies {
    implementation("dev.appspike:remote-config:1.4.5")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation(platform("androidx.compose:compose-bom:2025.05.01"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
}
