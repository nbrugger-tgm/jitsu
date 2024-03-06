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
}