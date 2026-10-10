rootProject.name = "jitsu"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

includeBuild("libs/jainparse")


includeBuild("libs/delegation-processor")

include(
    "compiler-utils",
    "parser",
    "compiler",
    "language-server",
    "gradle-plugin",
)

include(
    "backend:c"
)

include(
    "vs-extension",
)
