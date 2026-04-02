plugins {
    id("eu.nitok.jitsu.kotlin-library-conventions")
}

dependencies {
    implementation(project(":parser"))
    api(project(":compiler-utils"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
}
