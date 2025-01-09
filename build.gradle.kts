plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
    application
}

repositories { mavenCentral() }

application { mainClass.set("us.aadacio.demo.mcp.MainKt") }

group = "us.aadacio.demo.mcp"

version = "0.1.0"

dependencies {

    implementation(libs.slf4j.api)
    implementation(libs.slf4j.simple)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.sse)
    implementation(libs.kotlin.logging)
    implementation(libs.mcp.kotlin.sdk)
    testImplementation(kotlin("test"))
}

tasks.test { useJUnitPlatform() }

kotlin { jvmToolchain(21) }
