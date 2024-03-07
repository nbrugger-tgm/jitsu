package eu.nitok.jitsu.compiler.graph

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed interface Accessible<T: Accessible<T>> {
    @Transient val accessToSelf: MutableList<in Access<T>>
}

@Serializable
sealed interface Accessor {
    @Transient val accessFromSelf: List<Access<*>>
    val scope: Scope
}

@Serializable
sealed interface Access<T: Accessible<T>> {
    @Transient val target: T
    @Transient var accessor: Accessor

    sealed interface FunctionAccess : Access<Function>

    sealed interface VariableAccess : Access<Variable>
}
