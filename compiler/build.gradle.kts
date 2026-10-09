plugins {
    id("eu.nitok.jitsu.kotlin-library-conventions")
    id("com.google.devtools.ksp") version "2.3.12"
}

dependencies {
    ksp("eu.nitonfx.processors:delegation-processor-ksp")
    compileOnly("eu.nitonfx.processors:delegation-annotations")
    implementation(project(":parser"))
    api(project(":compiler-utils"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
}
