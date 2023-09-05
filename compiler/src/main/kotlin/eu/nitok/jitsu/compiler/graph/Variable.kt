package eu.nitok.jitsu.compiler.graph

import kotlinx.serialization.Serializable

@Serializable
open class Variable(
    open val spawnScope: Scope,
    val reassignable: Boolean = false,
    open val name: String,
    val declaredType: ResolvedType?,
    var actualType: ResolvedType?,
) : Accessible<Access.VariableAccess> {
    var initialized: Boolean = false
    override val accessToSelf: MutableList<Access.VariableAccess> = mutableListOf()
    fun read(accessor: Accessor) {
        accessToSelf.add(Access.VariableAccess.Read(this, accessor))
    }

    fun write(accessor: Accessor) {
        accessToSelf.add(Access.VariableAccess.Write(this, accessor))
    }
}