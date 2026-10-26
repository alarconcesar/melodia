plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

import java.util.Properties

android {
    namespace = "com.melodia.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.melodia.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 64
        versionName = "6.7"
        resConfigs("es")
    }
    signingConfigs {
        create("release") {
            val props = Properties()
            val f = rootProject.file("keystore.properties")
            if (f.exists()) {
                f.inputStream().use { props.load(it) }
                storeFile = file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        // Build "perf": mismo applicationId y misma firma que el APK debug (se
        // instala encima), pero NO debuggable. Un APK debug corre interpretado y
        // sin AOT: eso solo ya cuesta la mitad de los fps en Compose.
        create("perf") {
            initWith(getByName("debug"))
            isDebuggable = false
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    // Sin red en el VPS: lintVital intenta bajar sus jars y rompe el build offline.
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            // Un JVM por clase: los tests que instancian el ViewModel dejan
            // corrutinas de Room vivas y contaminaban la clase siguiente
            // (UncaughtExceptionsBeforeTest intermitente en aarch64).
            all { it.forkEvery = 1 }
        }
    }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.8" }
    kotlinOptions { jvmTarget = "17" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-session:1.2.1")
    implementation("androidx.media:media:1.7.0")
    implementation("dev.chrisbanes.haze:haze:0.3.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.26.5")

    // Arnés JVM (Robolectric): permite componer la UI en el VPS sin dispositivo
    testImplementation("junit:junit:4.13.2")
    // conscrypt no trae nativos para Linux aarch64 (VPS): fuera del classpath de test
    testImplementation("org.robolectric:robolectric:4.11.1") {
        exclude(group = "org.conscrypt", module = "conscrypt-openjdk-uber")
    }
    testImplementation("androidx.test:core-ktx:1.5.0")
    testImplementation("androidx.test.ext:junit-ktx:1.1.5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation(platform("androidx.compose:compose-bom:2024.01.00"))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
