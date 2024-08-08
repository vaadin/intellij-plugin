import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.21"
    id("org.jetbrains.intellij.platform") version "2.0.0"
}

val compatibilitySince = "233"
val compatibilityUntil = "242.*"

group = "com.vaadin"
val publishChannel = if (hasProperty("publishChannel")) {
    property("publishChannel") as String
} else {
    "Stable"
}

intellijPlatform {
    pluginConfiguration {
        version = if (hasProperty("projVersion")) {
            property("projVersion") as String
        } else {
            "1.0-SNAPSHOT"
        }
    }
    pluginVerification {
        ides {
            select {
                types = listOf(IntelliJPlatformType.IntellijIdeaCommunity)
                sinceBuild = compatibilitySince
                untilBuild = compatibilityUntil
            }
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        create("IC", "2023.3")
        bundledPlugin("com.intellij.java")

        pluginVerifier()
        zipSigner()
        instrumentationTools()
    }
}

tasks {
    patchPluginXml {
        sinceBuild.set(compatibilitySince)
        untilBuild.set(compatibilityUntil)
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        channels.set(listOf(publishChannel))
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
