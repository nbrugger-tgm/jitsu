package eu.nitok.jitsu.compiler.graph

import kotlinx.serialization.Transient

interface FunctionAware {
    @Transient var function: Function
}
