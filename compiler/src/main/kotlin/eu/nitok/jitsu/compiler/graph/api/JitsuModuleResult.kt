package eu.nitok.jitsu.compiler.graph.api

import eu.nitok.jitsu.common.CompilerMessages

/**
 * Result of loading or compiling a module graph, including diagnostics.
 */
data class JitsuModuleResult(
    val module: JitsuModule,
    val messages: CompilerMessages
)

