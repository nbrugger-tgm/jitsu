package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.ast.BiOperator
import eu.nitok.jitsu.compiler.ast.CompilerMessages
import eu.nitok.jitsu.compiler.ast.Located
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed interface Expression : Element {
    val isConstant: ReasonedBoolean;

    @Serializable
    data object Undefined : Expression {
        @Transient
        override val isConstant: ReasonedBoolean = ReasonedBoolean.False("No value is defined")

        @Transient
        override val children: List<Element> = listOf()
    }

    @Serializable
    data class Operation(val left: Expression, val operator: BiOperator, val right: Expression) : Expression {
        override val isConstant: ReasonedBoolean
            get() = if (left.isConstant is ReasonedBoolean.False) ReasonedBoolean.False("Left expression is not constant : ${(left.isConstant as ReasonedBoolean.False).message}")
            else if (right.isConstant is ReasonedBoolean.False) ReasonedBoolean.False("Right expression is not constant : ${(right.isConstant as ReasonedBoolean.False).message}")
            else ReasonedBoolean.True

        @kotlinx.serialization.Transient
        override val children: List<Element> = listOfNotNull(left, right)
    }

    @Serializable
    class VariableReference(override val reference: Located<String>) : Expression, Access.VariableAccess, ScopeAware {
        @Transient private lateinit var scope: Scope
        override val isConstant: ReasonedBoolean get() = ReasonedBoolean.False("Cuz not implemented yet")
        override val children: List<Element> get() = listOf()
        override var target: Variable? = null;
        @Transient
        override lateinit var accessor: Accessor

        override fun setEnclosingScope(parent: Scope) {
            this.scope = parent
        }
        override fun resolve(messages: CompilerMessages) {
            target = scope.resolveVariable(reference, messages)
        }
    }
}