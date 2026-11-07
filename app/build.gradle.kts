plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// El número de versión lo pone GitHub en cada compilación (-PversionCode=N)
val codigo = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: 1

android {
    namespace = "app.gestorarchivos"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.gestorarchivos"
        minSdk = 30
        targetSdk = 35
        versionCode = codigo
        versionName = "1.$codigo"
    }

    // La firma no está en el repositorio: GitHub la crea desde sus secretos (FIRMA_JKS y FIRMA_CLAVE)
    val llave = file("firma.jks")
    val clave = System.getenv("FIRMA_CLAVE")
    signingConfigs {
        if (llave.exists() && clave != null) {
            create("firma") {
                storeFile = llave
                storePassword = clave
                keyAlias = "gestor"
                keyPassword = clave
            }
        }
    }
    val firma = signingConfigs.findByName("firma") ?: signingConfigs.getByName("debug")

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = firma
        }
        debug {
            signingConfig = firma
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    val bom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(bom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-video:2.7.0")
}
