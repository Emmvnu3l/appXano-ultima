plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// buildDir personalizado eliminado para evitar bloqueos de archivos en Windows


android {
    namespace = "com.example.myapplication"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "AUTH_BASE_URL", "\"https://x8ki-letl-twmt.n7.xano.io/api:E0E2xd7q/\"")
        buildConfigField("String", "STORE_BASE_URL", "\"https://x8ki-letl-twmt.n7.xano.io/api:-51vSSC_/\"")
        buildConfigField("String", "USER_BASE_URL", "\"https://x8ki-letl-twmt.n7.xano.io/api:Az9iOmEB/\"")
        buildConfigField("int", "TOKEN_TTL_SEC", "3600")
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
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit.gson)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.recyclerview)
    implementation(libs.coil)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Compose eliminado según requerimiento
}
