package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.ast.Located
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable;

@Serializable
class Function(
    val bodyScope: Scope,
    val name: Located<String>?,
    val returnType: Type?,
    val parameters: List<Parameter>,
    val body: List<Instruction>
) : Accessor, Accessible<Access.FunctionAccess> {
    override val accessToSelf: MutableList<Access.FunctionAccess> = mutableListOf()
    override val accessFromSelf: MutableList<Access> = mutableListOf()
}

@Serializable
data class Parameter(
    val name: Located<String>,
    val type: Type,
    val defaultValue: Expression?
) {
    fun asVariable(fn: Function): Variable = Variable(false, name, type)
}
