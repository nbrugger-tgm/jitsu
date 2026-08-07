package eu.nitok.jitsu.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider

internal fun Project.createCapability(name: String): Provider<String> = provider {
    val group = group.toString().ifEmpty { path.removePrefix(":").replace(":", ".") }.ifEmpty { "default" }
    return@provider "$group:${name}-$name:${version}"
}
