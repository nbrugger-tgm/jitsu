package eu.nitok.jitsu.compiler.graph.elements

import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.common.locating.Location
import eu.nitok.jitsu.compiler.graph.api.Expression
import eu.nitok.jitsu.compiler.graph.api.Type
import eu.nitok.jitsu.compiler.graph.elements.types.TypeElement
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal sealed interface ExpressionElement {
    val asExpression: Expression
        get() = when (this) {
            is Expression -> this
            is ConstantElement<*> -> asConstant
            is ArrayLiteral -> this
            is UndefinedExpression -> this
            is FunctionCall -> this
            is VariableReference -> this
        }

    fun calculateType(
        context: Map<String, TypeElement>,
        messages: CompilerMessages,
        typeHint: TypeElement? = null
    ): TypeElement?

    val isConstant: ReasonedBoolean
    val location: Location
    val type: Type get() = typeElement.asType
    val typeElement: TypeElement
}