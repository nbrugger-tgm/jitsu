package eu.nitok.jitsu.compiler.graph.behaviour

import eu.nitok.jitsu.common.CompilerMessages

internal interface Restorable {
    fun restore(messages: CompilerMessages)
}