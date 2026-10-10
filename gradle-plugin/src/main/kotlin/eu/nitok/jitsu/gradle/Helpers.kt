package eu.nitok.jitsu.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider

/**
 * Builds the capability coordinates of a *feature variant* of this project.
 *
 * The format matches what [org.gradle.api.artifacts.ModuleDependencyCapabilitiesHandler.requireFeature]
 * derives on the consumer side, so producer and consumer agree without either of them hard coding
 * the other's coordinates.
 */
internal fun Project.featureCapability(feature: String): Provider<String> = provider {
    val group = group.toString().ifEmpty { path.removePrefix(":").replace(":", ".") }.ifEmpty { "default" }
    "$group:$name-$feature:$version"
}

internal fun stringProvider(provider: Provider<String>) = object {
    override fun toString(): String = provider.get()
}
