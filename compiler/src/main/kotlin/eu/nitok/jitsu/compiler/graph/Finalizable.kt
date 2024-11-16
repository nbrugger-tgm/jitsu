package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.ast.CompilerMessages

/**
 * An interface for elements that need to finalize their state after the whole graph has been built
 */
interface Finalizable {
    fun finalizeGraph(messages: CompilerMessages)
}