package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.ast.Located
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Variable(
    val reassignable: Boolean = false,
    override val name: Located<String>,
    val declaredType: Type?,
    private val initialValue: Expression?
) : Element, Accessible<Variable> {
    var implicitType: Type? = null
        internal set;
    val type: Type get() = declaredType ?: implicitType ?: Type.Undefined

    @Transient
    override val accessToSelf: MutableList<Access<Variable>> = mutableListOf()
    override val children: List<Element> get() = listOfNotNull(declaredType)
}