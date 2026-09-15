plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.16.1"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
}

dependencies {
    // JSON parsing
    implementation("org.json:json:20231013")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

sourceSets {
    main {
        java {
            exclude("test/**")
        }
    }
}

intellij {
    pluginName.set("offAiAgent")
    version.set(providers.gradleProperty("platformVersion").get())
    type.set(providers.gradleProperty("platformType").get())
    plugins.set(listOf())
    downloadSources.set(false)
}

tasks {
    patchPluginXml {
        sinceBuild.set(providers.gradleProperty("pluginSinceBuild").get())
        untilBuild.set(providers.gradleProperty("pluginUntilBuild").get())
    }

    buildSearchableOptions {
        enabled = false
    }

    buildPlugin {
        archiveFileName.set("offAiAgent-${version}.zip")
    }

    compileJava {
        options.encoding = "UTF-8"
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN") ?: "")
        privateKey.set(System.getenv("PRIVATE_KEY") ?: "")
        password.set(System.getenv("PRIVATE_KEY_PASSWORD") ?: "")
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN") ?: "")
    }
}
