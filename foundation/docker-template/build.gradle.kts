plugins {
    kotlin("jvm") version "2.0.21"
    application
    id("io.ktor.plugin") version "2.3.12"
}

group = "org.elevenetc.playground.paas"
version = "1.0.0"

application {
    mainClass.set("org.elevenetc.playground.paas.runtime.ApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core:2.3.12")
    implementation("io.ktor:ktor-server-netty:2.3.12")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
    implementation("ch.qos.logback:logback-classic:1.4.14")
}

ktor {
    docker {
        jreVersion.set(JavaVersion.VERSION_17)
        localImageName.set("function-runtime")
        imageTag.set("latest")
    }
}
