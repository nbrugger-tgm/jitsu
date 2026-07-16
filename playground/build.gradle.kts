plugins {
    id("eu.nitok.jitsu.kotlin-application-conventions")
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.html.builder)
    implementation(libs.ktor.server.netty)
    implementation(libs.kotlinx.serialization.json)
    runtimeOnly(libs.logback.classic)
    implementation(project(":compiler"))
    implementation(project(":parser"))
    implementation(project(":language-server"))
}

application {
    mainClass.set("eu.nitok.jitsu.playground.WebServerKt")
}