// Shared on-screen controller overlay, used by the SDL-hosted engine plugins
// (Ren'Py, KiriKiri, …). Framework-only UI: it builds a translucent touch pad
// and hands each press to an injector the plugin wires to its engine's input.
// No native code and no runtime — ABI-agnostic, like :app and :commons.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace  = "com.nuani.asobiri.gamepad"
    compileSdk = libs.versions.sdkCompile.get().toInt()

    defaultConfig {
        minSdk = libs.versions.sdkMin.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        allWarningsAsErrors = true
    }
}
