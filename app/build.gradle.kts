import java.util.Properties // Ensure this import is at the top

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.abhishekdadhich.movemate"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.abhishekdadhich.movemate"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Read API key from local.properties using Kotlin syntax
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { input ->
                localProperties.load(input)
            }
        }
        // Expose API key as a BuildConfig field
        buildConfigField(
            "String",
            "TFNW_API_KEY",
            "\"${localProperties.getProperty("TFNW_API_KEY", "")}\""
        )
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    // Add this block to enable BuildConfig generation
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    // Your existing core dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    // implementation(libs.androidx.core.splashscreen)

    // Test dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Retrofit for networking
    implementation(libs.retrofit)
    // Gson converter for Retrofit (to parse JSON)
    implementation(libs.converter.gson)

    // OkHttp
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // Kotlin Coroutines for asynchronous operations
    implementation(libs.kotlinx.coroutines.core) // Or latest stable
    implementation(libs.jetbrains.kotlinx.coroutines.android) // Or latest stable

    // Lifecycle KTX for lifecycleScope (to launch coroutines in Activity)
    implementation(libs.androidx.lifecycle.runtime.ktx) // Or latest stable
}