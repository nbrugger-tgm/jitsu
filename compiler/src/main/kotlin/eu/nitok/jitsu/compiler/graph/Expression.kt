package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.ast.BiOperator
import eu.nitok.jitsu.compiler.ast.CompilerMessages
import eu.nitok.jitsu.compiler.ast.Located
import eu.nitok.jitsu.compiler.parser.Range
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed interface Expression : Element {
    val isConstant: ReasonedBoolean;
    fun calculateType(context: Map<String, Type> = mapOf()): Type?
    val location: Range

    @Serializable
    data class Undefined(override val location: Range) : Expression {
        @Transient
        override val isConstant: ReasonedBoolean = ReasonedBoolean.False("No value is defined")
        override fun calculateType(context: Map<String, Type>): Type {
            return Type.Undefined
        }

        @Transient
        override val children: List<Element> = listOf()
    }

    @Serializable
    data class Operation(val left: Expression, val operator: BiOperator, val right: Expression) : Expression {
        override val isConstant: ReasonedBoolean
            get() = if (!left.isConstant.value) ReasonedBoolean.False(
                "Left expression is not constant",
                left.isConstant
            )
            else if (!right.isConstant.value) ReasonedBoolean.False(
                "Right expression is not constant",
                right.isConstant
            )
            else ReasonedBoolean.True("Left and right expressions are constant", right.isConstant, left.isConstant)

        override fun calculateType(context: Map<String, Type>): Type? {
            val leftType = left.calculateType(context)
            val rightType = right.calculateType(context)
            if (leftType == null || rightType == null) return null
            if (leftType != rightType) return null
            return leftType
        }

        override val location: Range
            get() = Range(left.location.start, right.location.end)

        @Transient
        override val children: List<Element> = listOfNotNull(left, right)
    }

    @Serializable
    class VariableReference(override val reference: Located<String>) : Expression, Access.VariableAccess, ScopeAware {
        @Transient
        private lateinit var scope: Scope
        override val isConstant: ReasonedBoolean get() = ReasonedBoolean.False("Cuz not implemented yet")
        override fun calculateType(context: Map<String, Type>): Type? {
            return context[reference.value] ?: scope.resolveVariable(reference, CompilerMessages())?.initialValue?.calculateType(context)
        }

        override val location: Range get() = reference.location

        override val children: List<Element> get() = listOf()

        @Transient
        override var target: Variable? = null;

        @Transient
        override lateinit var accessor: Accessor

        override fun setEnclosingScope(parent: Scope) {
            this.scope = parent
        }

        override fun resolveAccessTarget(messages: CompilerMessages): Variable? {
            return scope.resolveVariable(reference, messages)
        }
    }
}