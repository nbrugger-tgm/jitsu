package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.ast.BiOperator
import kotlinx.serialization.Serializable

@Serializable
sealed interface Expression {
    val isConstant: ReasonedBoolean;

    @Serializable
    data object Undefined: Expression {
        override val isConstant: ReasonedBoolean get() = ReasonedBoolean.False("No value is defined")
    }

    @Serializable
    data class Operation(val left: Expression, val operator: BiOperator, val right: Expression): Expression {
        override val isConstant: ReasonedBoolean
            get() = if(left.isConstant is ReasonedBoolean.False) ReasonedBoolean.False("Left expression is not constant : ${(left.isConstant as ReasonedBoolean.False).message}")
                    else if(right.isConstant is ReasonedBoolean.False) ReasonedBoolean.False("Right expression is not constant : ${(right.isConstant as ReasonedBoolean.False).message}")
                    else ReasonedBoolean.True
    }
}