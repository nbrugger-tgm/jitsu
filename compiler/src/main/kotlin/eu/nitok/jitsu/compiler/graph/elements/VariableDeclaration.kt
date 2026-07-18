package eu.nitok.jitsu.compiler.graph.elements

import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.compiler.graph.api.*
import eu.nitok.jitsu.compiler.graph.api.VariableDeclaration
import eu.nitok.jitsu.compiler.graph.elements.types.TypeElement
import eu.nitok.jitsu.compiler.graph.elements.types.Undefined
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal data class VariableDeclaration(
    override val reassignable: Boolean = false,
    override val name: Located<String>,
    override val declaredTypeElement: TypeElement?,
    override val initialValueElement: ExpressionElement?,
) : AccessibleElement<Variable>, VariableElement, InstructionElement, VariableDeclaration {
    override fun getSymbol(module: JitsuModule) = module.getSymbolID(this)
    override var symbolIndex: Int? = null
    @Transient override lateinit var module: JitsuModule
    override val implicitType: Type? get() = implicitTypeElement?.asType
    var implicitTypeElement: TypeElement? = null
        internal set

    override val initialValue: Expression? get() = initialValueElement?.asExpression
    override val type: Type get() = declaredType ?: implicitType ?: Undefined
    @Transient override val declaredType: Type? = declaredTypeElement?.asType
    @Transient
    override val accessToSelf: MutableList<Access<Variable>> = mutableListOf()
    override val children: List<Element> get() = listOfNotNull(declaredType, initialValue)
    override fun toString(): String {
        return "var ${name.value}${declaredType?.let { ": $it" }?:""}${initialValue?.let{" = $it"}?:""}"
    }

}