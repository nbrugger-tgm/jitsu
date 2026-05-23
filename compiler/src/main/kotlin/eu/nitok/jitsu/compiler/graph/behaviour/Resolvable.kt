package eu.nitok.jitsu.compiler.graph.behaviour

import eu.nitok.jitsu.common.CompilerMessages

internal interface Resolvable {
    fun resolve(messages: CompilerMessages)
}