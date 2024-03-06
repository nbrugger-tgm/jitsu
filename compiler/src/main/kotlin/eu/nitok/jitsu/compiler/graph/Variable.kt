package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.ast.Located
import kotlinx.serialization.Serializable

@Serializable
data class Variable(
    val reassignable: Boolean = false,
    val name: Located<String>,
    val declaredType: Type?,
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