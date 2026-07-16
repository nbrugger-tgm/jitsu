plugins {
    id("eu.nitok.jitsu.kotlin-application-conventions")
}

dependencies {
    implementation(libs.lsp4j)
    implementation(kotlin("reflect"))
    implementation(project(":compiler"))
    implementation(project(":parser"))
}

application {
    mainClass.set("LauncherKt")
}

tasks.run.configure {
    args("--tcp")
}