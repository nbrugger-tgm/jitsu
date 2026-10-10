rootProject.name = "stdlib"

pluginManagement {
    includeBuild("../")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}