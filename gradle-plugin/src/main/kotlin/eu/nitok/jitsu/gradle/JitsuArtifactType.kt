package eu.nitok.jitsu.gradle

import org.gradle.api.attributes.Attribute

val artifactType = Attribute.of("jitsu.artifactType", JitsuArtifactType::class.java)

/**
 * [org.gradle.api.attributes.Usage] of the semi-compiled Jitsu IR (`.jim`) modules.
 *
 * Jitsu IR variants carry a usage of their own so that they are never considered a candidate
 * when a native (`cplusplus-api`/`native-link`/`native-runtime`) resolution looks at a Jitsu project.
 */
const val JITSU_IR_USAGE: String = "jitsu-ir"

/**
 * Feature name of the native bindings variant every Jitsu module publishes.
 *
 * The bindings variant holds only the hand written C of a module (`src/<sourceSet>/c`) plus the
 * generated binding headers - not the compiled, auto generated C of the module itself. It is
 * published under a dedicated capability (`<group>:<module>-bindings`) instead of the default one,
 * so that:
 *  * plain C/C++ consumers transparently get the "full" library (generated code *and* bindings), and
 *  * Jitsu consumers can explicitly ask for the bindings only, because they re-generate the C of
 *    their dependencies from the IR and must not link the dependency's generated code twice.
 */
const val BINDINGS_FEATURE: String = "bindings"

enum class JitsuArtifactType {
    C,
    IR,
    SOURCE
}
