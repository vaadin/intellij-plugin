import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask

plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "1.9.21"
  id("org.jetbrains.intellij.platform") version "2.2.0"
  id("com.diffplug.spotless") version "7.0.0.BETA2"

  id("com.adarshr.test-logger") version "4.0.0"
}

// version for building plugin
val buildVersion = "2023.3"

// version for verifying plugin, check validation.yml
val verifyVersion =
    if (hasProperty("verifyVersion")) {
      property("verifyVersion") as String
    } else {
      buildVersion
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
  implementation("com.amplitude:java-sdk:[1.8.0,2.0)")
  implementation("org.json:json:20201115")
  implementation("com.vaadin:license-checker:1.13.3") {
    exclude(group = "net.java.dev.jna", module = "jna")
    exclude(group = "net.java.dev.jna", module = "jna-platform")
  }

  intellijPlatform {
    intellijIdeaUltimate(buildVersion)
    bundledPlugin("com.intellij.java")
    bundledPlugin("org.jetbrains.idea.maven")
    bundledPlugin("org.jetbrains.plugins.gradle")
    bundledPlugin("com.intellij.properties")
    bundledPlugin("com.intellij.microservices.jvm")
    bundledPlugin("JavaScript")

    pluginVerifier()
    zipSigner()

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
  pluginVerification {
    ides {
      ide(IntelliJPlatformType.IntellijIdeaCommunity, verifyVersion)
      ide(IntelliJPlatformType.IntellijIdeaUltimate, verifyVersion)
    }
    verificationReportsFormats = listOf(VerifyPluginTask.VerificationReportsFormats.MARKDOWN)
  }
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
