rootProject.name = "examples"

pluginManagement {
    includeBuild("../")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

includeBuild("../stdlib")
include("single-file-program")