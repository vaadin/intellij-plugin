plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.21"
    id("org.jetbrains.intellij.platform") version "2.0.0"
}

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
            recommended()
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
        sinceBuild.set("233")
        untilBuild.set("252")
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
