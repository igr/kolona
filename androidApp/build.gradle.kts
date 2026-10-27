import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "dev.oblac.kolona"
    compileSdk = 37
    defaultConfig {
        applicationId = "dev.oblac.kolona"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_17 } }
dependencies {
    implementation(project(":shared"))
    implementation(libs.activity.compose)
}

