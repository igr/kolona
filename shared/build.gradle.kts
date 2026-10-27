import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(17)
    jvm()
    androidLibrary {
        namespace = "dev.oblac.kolona.shared"
        compileSdk = 37
        minSdk = 24
        compilerOptions { jvmTarget = JvmTarget.JVM_17 }
        androidResources { enable = true }
        withHostTest { }
    }
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "KolonaShared"
            isStatic = true
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
        }
        commonTest.dependencies { implementation(kotlin("test")) }
        jvmMain.dependencies { implementation(compose.desktop.currentOs) }
    }
}

compose.resources {
    packageOfResClass = "dev.oblac.kolona.resources"
}

