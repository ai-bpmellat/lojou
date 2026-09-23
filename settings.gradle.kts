pluginManagement {
    repositories {
        // Try local cache first, then JetBrains CDN (sometimes bypasses TLS issues)
        maven { url = uri("https://cache-redirector.jetbrains.com/plugins.gradle.org/m2") }
        maven { url = uri("https://www.jetbrains.com/intellij-repository/releases") }
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "lojou"
