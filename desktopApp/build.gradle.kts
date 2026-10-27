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
            packageName = "Cuvar Kolone"
            // jpackage on macOS requires a positive first version component.
            packageVersion = "1.0.0"
            modules("java.prefs", "jdk.unsupported")
            macOS {
                bundleID = "dev.oblac.kolona"
                dockName = "Čuvar Kolone"
                infoPlist {
                    extraKeysRawXml = "<key>CFBundleDisplayName</key><string>Čuvar Kolone</string>"
                }
            }
        }
    }
}
