package eu.nitok.jitsu.compiler.graph.elements

import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.common.locating.Location
import eu.nitok.jitsu.compiler.graph.api.Element
import eu.nitok.jitsu.compiler.graph.api.Expression
import eu.nitok.jitsu.compiler.graph.elements.types.TypeElement
import eu.nitok.jitsu.compiler.graph.elements.types.Undefined
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal data class UndefinedExpression(override val location: Location) : ExpressionElement, Expression.Undefined {
    @Transient
    override val isConstant: ReasonedBoolean = ReasonedBoolean.False("No value is defined")
    override fun calculateType(context: Map<String, TypeElement>, messages: CompilerMessages, typeHint: TypeElement?): TypeElement {
        return type
    }

    @Transient override val type = Undefined
    @Transient override val typeElement: TypeElement = Undefined

    @Transient
    override val children: List<Element> = listOf()
}