plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.2.20"
}

group = "org.elevenetc.playground.paas"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin Compiler Embeddable for parsing
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.2.20")

    // Serialization for JSON output
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Testing
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
