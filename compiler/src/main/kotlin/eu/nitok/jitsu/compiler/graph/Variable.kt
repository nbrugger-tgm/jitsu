package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.ast.Located
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Variable(
    val reassignable: Boolean = false,
    val name: Located<String>,
    val declaredType: Type?,
) : Element, Accessible<Variable> {
    @Transient override val accessToSelf: MutableList<Access<Variable>> = mutableListOf()
    override val children: List<Element> get() = listOfNotNull(declaredType)
}