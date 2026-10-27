import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.playPublisher)
}

/** The Google Play upload key stays out of the repository, in keystore.properties next to this project. */
val keystore = rootProject.file("keystore.properties").takeIf { it.exists() }?.let { file ->
    Properties().apply { file.inputStream().use { load(it) } }
}

android {
    namespace = "dev.oblac.kolona"
    compileSdk = 37
    defaultConfig {
        applicationId = "dev.oblac.kolona"
        minSdk = 24
        targetSdk = 37
        // `just beta` passes the commit count, so each upload is higher than the last.
        versionCode = providers.gradleProperty("versionCode").orNull?.toInt() ?: 1
        versionName = "0.1.0"
    }
    signingConfigs {
        if (keystore != null) create("upload") {
            storeFile = file(keystore.getProperty("storeFile"))
            storePassword = keystore.getProperty("storePassword")
            keyAlias = keystore.getProperty("keyAlias")
            keyPassword = keystore.getProperty("keyPassword")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.findByName("upload")
        }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_17 } }

/** `just beta` uploads to internal testing with the Play service account key of sisyphus, in ~/.android outside the repository. */
play {
    serviceAccountCredentials.set(file(System.getProperty("user.home") + "/.android/sisyphus-play.json"))
    track.set("internal")
    defaultToAppBundles.set(true)
}
dependencies {
    implementation(project(":shared"))
    implementation(libs.activity.compose)
}

