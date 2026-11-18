plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// El checker de actualizaciones de F-Droid parsea este archivo con regex estáticos: necesita
// un versionCode numérico y un nombre de versión entre comillas dentro de `defaultConfig`.
// No evalúa variables, tags ni propiedades de Gradle; si esos valores son expresiones, no
// encuentra nada y nunca detecta la actualización. Mantener los literales y actualizarlos en
// el PR de release: el workflow verifica que coincidan con el tag antes de construir.
// Las propiedades de CI no se aceptan a ciegas: un valor mal formado aborta el build en lugar
// de caer en silencio al literal de `defaultConfig` y publicar una versión equivocada.
private val versionPattern = Regex("""^\d+\.\d+\.\d+(-[0-9A-Za-z]+(\.[0-9A-Za-z]+)*)?$""")

val overrideVersionCode =
    (findProperty("appVersionCode") as String?)?.let { raw ->
        raw.toIntOrNull() ?: error("appVersionCode debe ser numérico; se recibió '$raw'.")
    }

val overrideVersionName =
    (findProperty("appVersionName") as String?)?.trim()?.takeIf { it.isNotEmpty() }?.also {
        require(versionPattern.matches(it)) {
            "appVersionName inválido '$it'. Se espera MAJOR.MINOR.PATCH con sufijo opcional."
        }
    }

android {
    namespace = "com.cvc953.localplayer"
    compileSdk = 36
        composeOptions {
            kotlinCompilerExtensionVersion = libs.versions.kotlinCompilerExtensionVersion.get()
        }

    dependenciesInfo {
        // Desactiva la inclusión de metadatos de dependencias en el APK
        includeInApk = false
        includeInBundle = false
    }

    defaultConfig {
        applicationId = "com.cvc953.localplayer"
        minSdk = 24
        targetSdk = 36

        // Leídos por el checker de F-Droid. No convertir en expresiones.
        versionCode = 10106
        versionName = "1.1.6"

        // Overrides de CI. Se aplican después de los literales a propósito.
        overrideVersionCode?.let { versionCode = it }
        overrideVersionName?.let { versionName = it }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    // Signing config for Release using environment variables provided by CI
    val keystorePath = System.getenv("ANDROID_SIGNING_STORE_FILE")
    val keystorePassword = System.getenv("ANDROID_SIGNING_STORE_PASSWORD")
    val keyAlias = System.getenv("ANDROID_SIGNING_KEY_ALIAS")
    val keyPassword = System.getenv("ANDROID_SIGNING_KEY_PASSWORD")

    if (!keystorePath.isNullOrBlank() && !keystorePassword.isNullOrBlank() && !keyAlias.isNullOrBlank() && !keyPassword.isNullOrBlank()) {
        val resolvedKeystorePath = if (keystorePath.startsWith("app/")) keystorePath.removePrefix("app/") else keystorePath
        signingConfigs {
            create("release") {
                storeFile = file(resolvedKeystorePath)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
        buildTypes {
            getByName("release") {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.unit)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.animation.core)
    implementation(libs.androidx.appcompat)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Mocking for unit tests
    testImplementation(libs.mockk)
    testImplementation(libs.mockkandroid)

    // Implementación real de org.json para los unit tests (el stub de android.jar lanza excepciones)
    testImplementation(libs.org.json)
    // Implementación real de XmlPullParser para los unit tests: el stub de android.jar
    // lanza "Method newInstance in org.xmlpull.v1.XmlPullParserFactory not mocked".
    testImplementation(libs.kxml2)
    androidTestImplementation(libs.mockk)
    androidTestImplementation(libs.mockkandroid)

    // Third-party
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Media3
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")
    implementation("androidx.media3:media3-session:1.2.1")

    // MediaSession y compatibilidad con notificaciones
    implementation("androidx.media:media:1.7.0")

    // Compose Material icons
    implementation("androidx.compose.material:material-icons-extended:1.6.0")

    // ViewModel + Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // StateFlow + collectAsState
    implementation("androidx.compose.runtime:runtime-livedata")

    // Palette para extraer color dominante
    implementation("androidx.palette:palette:1.0.0")

    // Lectura de metadatos de audio (letras embebidas)
    implementation("net.jthink:jaudiotagger:3.0.1")
}
