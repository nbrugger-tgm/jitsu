package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.Located
import eu.nitok.jitsu.common.Range
import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.compiler.model.BiOperator
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed interface Expression : Element {
    val isConstant: ReasonedBoolean;
    fun calculateType(context: Map<String, Type>, messages: CompilerMessages, typeHint: Type? = null): Type?
    val location: Range
    val type: Type

    @Serializable
    data class Undefined(override val location: Range) : Expression {
        @Transient
        override val isConstant: ReasonedBoolean = ReasonedBoolean.False("No value is defined")
        override fun calculateType(context: Map<String, Type>, messages: CompilerMessages, typeHint: Type?): Type {
            return type
        }

        override val type = Type.Undefined

        @Transient
        override val children: List<Element> = listOf()
    }

    @Serializable
    class Operation(val left: Expression, val operator: Located<BiOperator>, val right: Expression) :
        Expression,
        ScopeAware,
        Access.FunctionAccess {
        val functionCall = Instruction.FunctionCall(
            operator.map { it.functionName },
            listOf(left, right),
            left.location.rangeTo(right.location),
        )
        override val isConstant: ReasonedBoolean get() = functionCall.isConstant

        override fun calculateType(
            context: Map<String, Type>,
            messages: CompilerMessages,
            typeHint: Type?
        ) = functionCall.calculateType(context, messages, typeHint)

        override val location: Range
            get() = functionCall.location
        override val type: Type
            get() = functionCall.type
        override val children: List<Element>
            get() = functionCall.children

        override fun setEnclosingScope(parent: Scope) = functionCall.setEnclosingScope(parent)

        override var target: Function?
            get() = functionCall.target
            set(value) {
                functionCall.target = value
            }
        override var accessor: Accessor
            get() = functionCall.accessor
            set(value) {
                functionCall.accessor = value
            }
        override val reference: Located<String>
            get() = functionCall.reference

        override fun resolveAccessTarget(messages: CompilerMessages): Function? {
            return functionCall.resolveAccessTarget(messages)
        }
    }

    @Serializable
    class VariableReference(override val reference: Located<String>) : Expression, Access.VariableAccess, ScopeAware {
        @Transient
        private lateinit var scope: Scope
        override val isConstant: ReasonedBoolean get() = ReasonedBoolean.False("Cuz not implemented yet")
        override fun calculateType(context: Map<String, Type>, messages: CompilerMessages, typeHint: Type?): Type? {
            val type = context[reference.value]
                ?: target?.declaredType
                ?: target?.initialValue?.calculateType(context, messages)
            if (type != null) this.type = type
            return type
        }

        override lateinit var type: Type
            private set;
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

        override lateinit var accessKind: Access.VariableAccess.AccessKind
    }

    @Serializable
    data class ArrayLiteral(val elements: List<Expression>, override val location: Range) : Expression {
        override val isConstant: ReasonedBoolean
            get() = elements.map { it.isConstant }.reduce { acc, boolean -> acc.and(boolean) }

        override fun calculateType(
            context: Map<String, Type>,
            messages: CompilerMessages,
            typeHint: Type?
        ): Type.Array {
            val type = (if (elements.isEmpty()) {
                if (typeHint is Type.Array) typeHint.elementType else {
                    messages.error("Cannot infer type of empty array", this.location)
                    Type.Undefined
                }
            } else elements.mapNotNull { element ->
                element.calculateType(context, messages, typeHint?.let { it as? Type.Array }?.elementType)
            }.reduce { acc, type ->
                val aToB = acc.acceptsInstanceOf(type)
                if (aToB.value) acc
                else {
                    val bToA = type.acceptsInstanceOf(acc)
                    if (bToA.value) type
                    else Type.Union(listOf(acc, type))
                }
            }).let { Type.Array(it, elements.size, 1) }
            this.type = type
            return type
        }

        override lateinit var type: Type.Array
        override val children: List<Element> get() = elements

    }
}