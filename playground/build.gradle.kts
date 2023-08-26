plugins {
    id("eu.nitok.jitsu.kotlin-application-conventions")
}

dependencies {
    implementation("io.ktor:ktor-server-core:2.3.3")
    implementation("io.ktor:ktor-server-html-builder:2.3.3")
    implementation("io.ktor:ktor-server-netty:2.3.3")
    runtimeOnly("ch.qos.logback:logback-classic:1.4.11")
    implementation(project(":compiler"))
}

application {
    mainClass.set("eu.nitok.jitsu.playground.WebServer")
}