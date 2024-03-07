package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.ast.Located
import eu.nitok.jitsu.compiler.model.sequence
import kotlinx.serialization.Serializable;
import kotlinx.serialization.Transient

@Serializable
class Function(
    override val scope: Scope,
    override val name: Located<String>?,
    val returnType: Type?,
    val parameters: List<Parameter>,
    val body: List<Instruction>,
) : Element, Accessor, Accessible<Function> {

    override val children: List<Element> get() = listOfNotNull(returnType) + parameters

    @Transient
    override val accessToSelf: MutableList<Access<Function>> = mutableListOf()

    @Transient
    override val accessFromSelf: List<Access<*>> = body.flatMap {
        it.sequence().filterIsInstance<Access<*>>()
    }.onEach { it.accessor = this }

    @Serializable
    data class Parameter(
        val name: Located<String>,
        val type: Type,
        val defaultValue: Expression?
    ) : Element {
        fun asVariable(fn: Function): Variable = Variable(false, name, type)
        override val children: List<Element> get() = listOfNotNull(type, defaultValue)
    }
}
