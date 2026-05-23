package eu.nitok.jitsu.compiler.graph.elements

import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.common.locating.Location
import eu.nitok.jitsu.compiler.graph.api.*
import eu.nitok.jitsu.compiler.graph.behaviour.ScopeAware
import eu.nitok.jitsu.compiler.graph.elements.types.TypeElement
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


@Serializable
internal class VariableReference private constructor(private val access: AccessElement.VariableAccessElement) : AccessElement.VariableAccess by access,
    Expression.VariableReference, Access.VariableAccess, ScopeAware, ExpressionElement {

    constructor(reference:  Located<String>) : this(AccessElement.VariableAccessElement(reference))

    override val isConstant: ReasonedBoolean get() = ReasonedBoolean.False("Cuz not implemented yet")
    override fun calculateType(context: Map<String, TypeElement>, messages: CompilerMessages, typeHint: TypeElement?): TypeElement? {
        val type = context[reference.value]
            ?: access.targetElement?.declaredTypeElement
            ?: access.targetElement?.initialValueElement?.calculateType(context, messages)
        if (type != null)
            this.typeElement = type
        return type
    }

    override lateinit var typeElement: TypeElement
        private set;
    override val type: Type get() = typeElement.asType
    override val location: Location get() = reference.location

    override val children: List<Element> get() = listOf()


    override lateinit var accessKind: Access.VariableAccess.AccessKind
}