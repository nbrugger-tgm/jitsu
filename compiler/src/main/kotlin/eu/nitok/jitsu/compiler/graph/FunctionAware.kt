package eu.nitok.jitsu.compiler.graph

interface FunctionAware {
    fun setEnclosingFunction(parent: Function)
}
