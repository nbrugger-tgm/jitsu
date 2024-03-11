package eu.nitok.jitsu.compiler.graph

import kotlinx.serialization.Transient

interface FunctionAware {
    fun setEnclosingFunction(parent: Function)
}
