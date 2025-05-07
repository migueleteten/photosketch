plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.photosketch3"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.photosketch3"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    packaging {
        resources {
            // Excluimos el archivo específico que da el error
            excludes.add("META-INF/DEPENDENCIES")
            // Añadimos exclusiones comunes para evitar problemas similares
            excludes.add("META-INF/LICENSE")
            excludes.add("META-INF/LICENSE.txt")
            excludes.add("META-INF/license.txt")
            excludes.add("META-INF/NOTICE")
            excludes.add("META-INF/NOTICE.txt")
            excludes.add("META-INF/notice.txt")
            excludes.add("META-INF/ASL2.0")
            excludes.add("META-INF/*.kotlin_module")
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
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // implementation(libs.google.play.services.auth)
    implementation(libs.androidx.credentials) // <-- Añade esta
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid) // <-- Añade esta (asegúrate que el alias coincida con el TOML)
    implementation(libs.google.api.client.android)
    implementation(libs.google.api.services.sheets)
    implementation(libs.google.http.client.android)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.camera.core)       // <-- Añade esta
    implementation(libs.androidx.camera.camera2)    // <-- Añade esta
    implementation(libs.androidx.camera.lifecycle)  // <-- Añade esta
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.androidx.exifinterface)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}