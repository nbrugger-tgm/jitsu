package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.ast.Located
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class VariableDeclaration(
    override val reassignable: Boolean = false,
    override val name: Located<String>,
    override val declaredType: Type?,
    override val initialValue: Expression?
) : Variable, Instruction {
    var implicitType: Type? = null
        internal set;
    val type: Type get() = declaredType ?: implicitType ?: Type.Undefined

    @Transient
    override val accessToSelf: MutableList<Access<Variable>> = mutableListOf()
    override val children: List<Element> get() = listOfNotNull(declaredType, initialValue)
}

interface Variable : Accessible<Variable>, Element{
    override val name: Located<String>
    val reassignable: Boolean
    val declaredType: Type?
    val initialValue: Expression?
}