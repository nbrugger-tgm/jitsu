package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.ast.BiOperator
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
    class VariableReference(val name: Located<String>) : Expression, Access.VariableAccess {
        @Transient
        override val isConstant: ReasonedBoolean = ReasonedBoolean.False("Cuz not implemented yet")
        override val children: List<Element> get() = listOf()
        lateinit var variable: Variable;
        override val target: Variable get() = variable;
        @Transient
        private lateinit var _accessor: Accessor
        override var accessor: Accessor
            get() = _accessor
            set(value) {
                _accessor = value
                variable = (accessor.scope.resolveVariable(name) ?: Variable(false, name, Type.Undefined))
                    .apply { accessToSelf.add(this@VariableReference) }
            }
        override val reference: Located<String>
            get() = name
    }
}