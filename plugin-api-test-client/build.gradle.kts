plugins {
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.6"
    id("java")
}

group = "com.vaadin.plugin"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web:3.4.2")
    implementation("org.springframework.boot:spring-boot-starter-security:3.4.2")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:3.4.2")
    implementation("com.vaadin:vaadin-spring:24.6.5")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    implementation("com.vaadin:vaadin-core:24.6.5")
    implementation("jakarta.persistence:jakarta.persistence-api:3.2.0")
    implementation("com.flowingcode.vaadin.addons:day-of-week-selector-addon:1.1.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.4.2")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("PASSED", "FAILED", "SKIPPED")
    }
}
