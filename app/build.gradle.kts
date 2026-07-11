import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Pixabay API key for the online image search: never hardcoded (this repo is public). Locally,
// put PIXABAY_API_KEY=... in local.properties (gitignored); CI supplies it via a Gradle
// property (-PPIXABAY_API_KEY=...) sourced from a GitHub Actions secret.
val pixabayApiKey: String = (project.findProperty("PIXABAY_API_KEY") as String?)
    ?: run {
        // Not `java.util.Properties()`: the Android plugin injects a `java` extension accessor
        // into this script's scope that shadows the `java.*` package prefix here.
        val localProps = Properties()
        val localFile = rootProject.file("local.properties")
        if (localFile.exists()) localFile.inputStream().use { localProps.load(it) }
        localProps.getProperty("PIXABAY_API_KEY", "")
    }

android {
    namespace = "com.dsk.soniloko"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dsk.soniloko"
        minSdk = 26
        targetSdk = 35
        versionCode = 10
        versionName = "1.10"
        buildConfigField("String", "PIXABAY_API_KEY", "\"$pixabayApiKey\"")
    }

    buildTypes {
        release {
            // NewPipeExtractor pulls in Mozilla Rhino, whose R8 shrinking issues (missing
            // desktop-only java.beans/javax.script classes) can go deeper than a couple of
            // -dontwarn rules fix. Not a Play Store app, so a slightly larger APK is an easy
            // trade for a build that doesn't fight R8 every release — same call DSK LoFi made.
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.newpipe)
    implementation(libs.okhttp)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    debugImplementation(libs.androidx.ui.tooling)
}
