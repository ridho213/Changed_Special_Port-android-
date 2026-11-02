plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.changedspecial.port"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.changedspecial.port"
        minSdk = 24; targetSdk = 34
        versionCode = 1; versionName = "1.0"
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
    }
    sourceSets["main"].jniLibs.srcDirs("src/main/jniLibs")
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    packaging { jniLibs { useLegacyPackaging = true } }   // .so harus ekstrak (ANGLE dimuat via path)
}
dependencies { implementation("androidx.core:core-ktx:1.13.1") }
