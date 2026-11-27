import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
}

/**
 * Firma release: credenciales y ruta del keystore en local.properties
 * (ignorado por Git). Un clone sin keystore sigue compilando en debug.
 */
val keystoreProps = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}
val releaseStoreFile = keystoreProps.getProperty("release.keystore.file")?.let { rootProject.file(it) }
val hayFirmaRelease = releaseStoreFile != null && releaseStoreFile!!.exists()

// URL del backend. Por defecto apunta a Render (produccion).
// Para usar el backend local:
//   .\gradlew.bat :app:assembleDebug -PBACKEND_URL=http://10.0.2.2:8080/
//   (10.0.2.2 = localhost del PC visto desde el emulador)
val backendUrl: String = (project.findProperty("BACKEND_URL") as String?)
    ?: "https://app-movil-ev-2.onrender.com/"

android {
    // El codigo vive en com.example.myapplication: alineamos el namespace
    // para que BuildConfig y R queden en el mismo paquete que las clases.
    // applicationId se mantiene para no cambiar la identidad de la app instalada.
    namespace = "com.example.myapplication"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.ep2"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // URL base de Retrofit, leida en RetrofitClient.kt
        buildConfigField("String", "BASE_URL", "\"$backendUrl\"")
    }

    signingConfigs {
        create("release") {
            releaseStoreFile?.let { archivo ->
                storeFile = archivo
                storePassword = keystoreProps.getProperty("release.keystore.password")
                keyAlias = keystoreProps.getProperty("release.key.alias")
                keyPassword = keystoreProps.getProperty("release.key.password")
            }
        }
    }

    buildTypes {
        release {
            if (hayFirmaRelease) {
                signingConfig = signingConfigs.getByName("release")
            }
            // R8 con packageScope rompia Kotlin en runtime (IllegalAccessError
            // en MainActivity.onCreate). El EFT pide firmado, no minificado.
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    @Suppress("DEPRECATION")
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.room.runtime)
    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    // --- Red: conexion con el microservicio Spring Boot ---
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}