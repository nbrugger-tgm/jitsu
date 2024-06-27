package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.ast.CompilerMessages
import eu.nitok.jitsu.compiler.ast.Located
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed interface Accessible<T : Accessible<T>> {
    @Transient
    val accessToSelf: MutableList<in Access<T>>
    val name: Located<String>?
}

@Serializable
sealed interface Accessor {
    @Transient
    val accessFromSelf: List<Access<*>>
}

fun findAccessesFromSelf(elems: Iterable<Element>): List<Access<*>> {
    val lst = mutableListOf<Access<*>>()
    fun findAccesses(inst: Element) {
        if (inst is Access<*>) lst.add(inst)
        if (inst !is Accessor) inst.children.forEach(::findAccesses)
    }
    elems.forEach(::findAccesses)
    return lst
}

@Serializable
sealed interface Access<T : Accessible<T>> {
    @Transient
    var target: T?
    @Transient
    var accessor: Accessor
    @Transient
    val reference: Located<String>

    fun resolve(messages: CompilerMessages): T?
    fun finalize(messages: CompilerMessages) {
        target = resolve(messages)
        target?.accessToSelf?.add(this)
    }

    sealed interface FunctionAccess : Access<Function>
    sealed interface VariableAccess : Access<Variable>
    sealed interface TypeAccess : Access<TypeDefinition>
}
