package eu.nitok.jitsu.gradle

import org.gradle.api.attributes.Attribute

val artifactType = Attribute.of("jitsu.artifactType", JitsuArtifactType::class.java)
enum class JitsuArtifactType {
    C,
    IR,
    SOURCE
}