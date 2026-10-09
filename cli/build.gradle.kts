plugins {
    id("eu.nitok.jitsu.kotlin-application-conventions")
}

application {
    mainClass.set("eu.nitok.jitsu.compiler.cli.CliKt")
}

dependencies {
    implementation(project(":compiler"))
    implementation(project(":parser"))
    project.project(":backend").subprojects {
        implementation(this)
    }
}