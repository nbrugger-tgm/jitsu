package eu.nitok.jitsu.compiler.graph

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable;

@Serializable
class Function(
    val bodyScope: Scope,
    val name: String?,
) : Accessor, Accessible<Access.FunctionAccess> {
    init {
        bodyScope.parent?.register(this)
    }
    val parameters: MutableList<Parameter> = mutableListOf()
    override val accessToSelf: MutableList<Access.FunctionAccess> = mutableListOf()
    override val accessFromSelf: MutableList<Access> = mutableListOf()
}

@Serializable
data class Parameter(
    val owner: Function,
    val name: String,
    val type: ResolvedType,
    val defaultValue: Constant<@Contextual Any>?
) {
    val variable: Variable get() = Variable(owner.bodyScope, false, name, type, null)
}
