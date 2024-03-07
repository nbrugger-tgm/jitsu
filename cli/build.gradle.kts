plugins {
    id("eu.nitok.jitsu.kotlin-application-conventions")
}

dependencies {
    implementation(project(":compiler"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    kapt("info.picocli:picocli-codegen:4.6.1")
    implementation("info.picocli:picocli:4.6.1")
    testImplementation(kotlin("test"))
}