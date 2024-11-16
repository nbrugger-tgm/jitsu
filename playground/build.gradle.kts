plugins {
    id("eu.nitok.jitsu.kotlin-application-conventions")
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:2.3.4")
    implementation("io.ktor:ktor-server-html-builder:2.3.4")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    runtimeOnly("ch.qos.logback:logback-classic:1.4.11")
    implementation(project(":compiler"))
    implementation(project(":language-server"))
}

application {
    mainClass.set("eu.nitok.jitsu.playground.WebServerKt")
}