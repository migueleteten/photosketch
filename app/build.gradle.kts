import org.gradle.kotlin.dsl.implementation
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
}

val keystorePropertiesFile = rootProject.file("keystore.properties") // Ahora en la raíz del proyecto
val keystoreProperties = Properties()
var signingConfigured = false
if (keystorePropertiesFile.exists() && keystorePropertiesFile.isFile) { // Añade isFile por si acaso
    try {
        keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
        if (keystoreProperties.getProperty("storeFile") != null &&
            keystoreProperties.getProperty("storePassword") != null &&
            keystoreProperties.getProperty("keyAlias") != null &&
            keystoreProperties.getProperty("keyPassword") != null) {
            signingConfigured = true
            println("INFO: keystore.properties cargado y completo.")
        } else {
            println("WARN: keystore.properties está incompleto. Faltan propiedades.")
            signingConfigured = false
        }
    } catch (e: Exception) {
        println("WARN: Error cargando keystore.properties: ${e.message}")
        signingConfigured = false
    }
} else {
    println("WARN: keystore.properties NO encontrado en la RAÍZ del proyecto. ('${keystorePropertiesFile.absolutePath}')")
    signingConfigured = false
}

android {
    namespace = "es.ace.photosketchapp2121"
    compileSdk = 35

    defaultConfig {
        applicationId = "es.ace.photosketchapp2121"
        minSdk = 24
        targetSdk = 35
        versionCode = 5
        versionName = "1.0.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (signingConfigured) {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            } else {
                // No poner nada aquí o un placeholder para que el build falle si no hay config
                // Esto es para forzar que use nuestra config o falle
                // Opcional: podrías lanzar una excepción si !signingConfigured
                // throw new GradleException("Signing config 'release' requires keystore.properties to be correctly set up.")
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Esta línea es la que le dice al build de release que use la config "release"
            signingConfig = signingConfigs.getByName("release")
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
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.google.api.client.android)
    implementation(libs.google.api.services.sheets)
    implementation(libs.google.http.client.android)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)    // Para soporte de Coroutines y Flow
    ksp(libs.androidx.room.compiler)         // Importante usar ksp aquí, no implementation
    // --- AÑADE O VERIFICA ESTA ---
    implementation(libs.google.api.services.drive)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}