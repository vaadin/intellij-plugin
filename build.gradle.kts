import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "1.9.21"
  id("org.jetbrains.intellij.platform") version "2.1.0"
  id("com.diffplug.spotless") version "7.0.0.BETA2"

  id("com.adarshr.test-logger") version "4.0.0"
}

group = "com.vaadin"

val publishChannel =
    if (hasProperty("publishChannel")) {
      property("publishChannel") as String
    } else {
      "Stable"
    }

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}

kotlin { jvmToolchain(17) }

repositories {
  mavenCentral()
  intellijPlatform { defaultRepositories() }
}

dependencies {
  intellijPlatform {
    intellijIdeaUltimate("2023.3")
    bundledPlugin("com.intellij.java")
    bundledPlugin("org.jetbrains.idea.maven")
    bundledPlugin("org.jetbrains.plugins.gradle")
    bundledPlugin("com.intellij.properties")
    bundledPlugin("com.intellij.microservices.jvm")

    pluginVerifier()
    zipSigner()
    instrumentationTools()

    testFramework(TestFrameworkType.Platform)
  }

  testImplementation(kotlin("test"))
  testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
  pluginConfiguration {
    version =
        if (hasProperty("projVersion")) {
          property("projVersion") as String
        } else {
          "1.0-SNAPSHOT"
        }
  }
  pluginVerification { ides { recommended() } }
}

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
  kotlin {
    // by default the target is every '.kt' and '.kts` file in the java sourcesets
    ktfmt("0.51").googleStyle().configure {
      it.setMaxWidth(120)
      it.setBlockIndent(4)
      it.setContinuationIndent(4)
      it.setRemoveUnusedImports(true)
      it.setManageTrailingCommas(false)
    }
  }
  kotlinGradle {
    target("*.gradle.kts") // default target for kotlinGradle
    ktfmt()
  }
}

tasks {
  patchPluginXml {
    sinceBuild.set("233")
    untilBuild.set("252.*")
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

  test { useJUnitPlatform() }
}
