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

    fun resolveAccessTarget(messages: CompilerMessages): T?
    fun finalize(messages: CompilerMessages) {
        target = resolveAccessTarget(messages)
        target?.accessToSelf?.add(this)
    }

    sealed interface FunctionAccess : Access<Function>
    sealed interface VariableAccess : Access<Variable> {
        val accessKind : AccessKind
        enum class AccessKind {
            /**
             * The parameter does not leave the scope and the callee can still use/manage the variable afterward
             */
            BORROW,

            /**
             * The parameter needs to be managed by the function and the callee needs to either give controll to the function
             * or hand the function a copy.
             */
            MOVE
        }
    }
    sealed interface TypeAccess : Access<TypeDefinition>
}
