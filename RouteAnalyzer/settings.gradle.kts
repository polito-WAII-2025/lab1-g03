pluginManagement {
    repositories {
        gradlePluginPortal() // ✅ This is where the plugin portal belongs!
        mavenCentral()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "RouteAnalyzer"
