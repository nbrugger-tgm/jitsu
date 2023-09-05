import com.github.gradle.node.npm.task.NpmTask

plugins {
    id("com.github.node-gradle.node") version "7.0.0"
}

tasks.register("build", NpmTask::class) {
    dependsOn("npmInstall")
    args = listOf("run","compile")
    inputs.files(fileTree("src"))
    inputs.files("tsconfig.json")

    outputs.dir("${layout.buildDirectory}/ts-out")
}

tasks.register("runIde", Exec::class) {
    dependsOn("build")
    dependsOn(":language-server:installDist")
    commandLine("code", "--extensionDevelopmentPath=${projectDir.absolutePath}", "../examples/")
}

node {
    download = true
    version = "18.17.1"
}