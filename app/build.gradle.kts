plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.basicandroidapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.basicandroidapp"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.core:core:1.12.0")
}
