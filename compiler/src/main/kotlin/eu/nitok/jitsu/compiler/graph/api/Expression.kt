package eu.nitok.jitsu.compiler.graph.api

import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.common.locating.Location
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
sealed interface Expression : Element {
    val isConstant: ReasonedBoolean
    val location: Location
    val type: Type

    interface Undefined: Expression
    interface VariableReference : Expression, Access.VariableAccess
    interface ArrayLiteral : Expression {
        val elements: List<Expression>
    }
    sealed interface Constant<out T> : Expression {
        val value: T
        val literal: String

        interface IntConstant: Constant<Long>
        interface UIntConstant: Constant<ULong>
        interface BooleanConstant: Constant<Boolean>
        interface StringConstant: Constant<String>
    }
}