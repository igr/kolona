plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}
kotlin { jvmToolchain(17) }
dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(libs.coroutines.swing)
}
compose.desktop {
    application {
        mainClass = "dev.oblac.kolona.desktop.MainKt"
        nativeDistributions {
            // Keep the executable name ASCII to avoid macOS signing/Unicode normalization issues.
            packageName = "Protest Defender"
            // jpackage on macOS requires a positive first version component.
            packageVersion = "1.0.0"
            modules("java.prefs", "jdk.unsupported")
            macOS {
                bundleID = "dev.oblac.kolona"
                dockName = "Protest Defender"
                infoPlist {
                    extraKeysRawXml = "<key>CFBundleDisplayName</key><string>Protest Defender</string>"
                }
            }
        }
    }
}
