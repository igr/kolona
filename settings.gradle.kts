pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        // gamekit, the library of the games in ~/prj/igr/gamekit, comes from the local Maven repository: see its README.
        mavenLocal { content { includeGroup("dev.oblac.gamekit") } }
        google()
        mavenCentral()
    }
}
rootProject.name = "kolona"
include(":shared", ":androidApp", ":desktopApp")

