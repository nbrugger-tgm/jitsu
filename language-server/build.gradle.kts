plugins {
    id("eu.nitok.jitsu.kotlin-application-conventions")
}

dependencies {
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.21.1")
    implementation(project(":compiler"))
}

application {
    mainClass.set("LauncherKt")
}