package eu.nitok.jitsu.gradle

import org.gradle.api.attributes.Attribute

val artifactType = Attribute.of("jitsu.artifactType", JitsuArtifactType::class.java)
val nativeArtifactKind = Attribute.of("eu.nitok.jitsu.nativeArtifactKind", String::class.java)

internal const val BINDINGS_NATIVE_ARTIFACT = "bindings"

enum class JitsuArtifactType {
    C,
    IR,
    SOURCE
}
