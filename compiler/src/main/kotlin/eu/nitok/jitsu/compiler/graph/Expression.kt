package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.common.*
import eu.nitok.jitsu.compiler.model.BiOperator
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed interface Expression : Element {
    val isConstant: ReasonedBoolean;
    fun calculateType(context: Map<String, Type>, messages: CompilerMessages): Type?
    val location: Range
    val type: Type

    @Serializable
    data class Undefined(override val location: Range) : Expression {
        @Transient
        override val isConstant: ReasonedBoolean = ReasonedBoolean.False("No value is defined")
        override fun calculateType(context: Map<String, Type>, messages: CompilerMessages): Type {
            return type
        }

        override val type = Type.Undefined

        @Transient
        override val children: List<Element> = listOf()
    }

    @Serializable
    data class Operation(val left: Expression, val operator: Located<BiOperator>, val right: Expression) :
        Expression,
        ScopeAware,
        Access.FunctionAccess {
        private lateinit var scope: Scope
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

        override fun calculateType(context: Map<String, Type>, messages: CompilerMessages): Type? {
            if (target == null) {
                val leftType = Located(left.calculateType(context, messages) ?: Type.Undefined, left.location)
                val rightType = Located(right.calculateType(context, messages) ?: Type.Undefined, right.location)
                target = scope.resolveFunction(
                    operator.map { it.functionName },
                    arrayOf(leftType, rightType),
                    messages
                )
            }
            val type = target?.returnType?.value
            if (type != null) this.type = type
            return type
        }

        override lateinit var type: Type
            internal set;

        override val location: Range
            get() = Range(left.location.start, right.location.end)

        @Transient
        override val children: List<Element> = listOfNotNull(left, right)
        override fun setEnclosingScope(parent: Scope) {
            this.scope = parent
        }

        override var target: Function? = null

        @Transient
        override lateinit var accessor: Accessor
        override val reference: Located<String> = operator.map { it.rune }

        override fun resolveAccessTarget(messages: CompilerMessages): Function? {
            //see FunctionCall#resolveAccessTarget for more information
            return target;
        }

        fun asFunctionCall(): Instruction.FunctionCall {
            val call = Instruction.FunctionCall(
                reference = operator.map { it.functionName },
                callParameters = listOf(left, right),
                location = reference.location
            )
            call.scope = scope
            call.target = target
            return call
        }
    }

    @Serializable
    class VariableReference(override val reference: Located<String>) : Expression, Access.VariableAccess, ScopeAware {
        @Transient
        private lateinit var scope: Scope
        override val isConstant: ReasonedBoolean get() = ReasonedBoolean.False("Cuz not implemented yet")
        override fun calculateType(context: Map<String, Type>, messages: CompilerMessages): Type? {
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
            messages: CompilerMessages
        ): Type? {
            if (elements.size == 0) TODO("STDLIB: Any type or generic fitting")
            if (elements.size == 1) return elements.single().calculateType(context, messages)
            val type = elements.map {
                (it to (it.calculateType(context, messages) ?: Type.Undefined)) as Pair<Expression, Type>?
            }.reduce { acc, type ->
                if (acc == null) return@reduce acc;
                if (type == null) return@reduce acc;
                val aToB = acc.second.acceptsInstanceOf(type.second)
                if (aToB.value) acc
                else {
                    val bToA = type.second.acceptsInstanceOf(acc.second)
                    if (bToA.value) type
                    else {
                        messages.error(
                            "No common type found between ${acc.second} and ${type.second}. Array literal elements must share a common type",
                            acc.first.location.rangeTo(type.first.location),
                            CompilerMessage.Hint(aToB.fullMessageChain().first, acc.first.location),
                            CompilerMessage.Hint(bToA.fullMessageChain().first, type.first.location)
                        )
                        null
                    }
                }
            }?.let { Type.Array(it.second, null, 1) }
            type?.let { this.type = it }
            return type
        }

        override lateinit var type: Type.Array
        override val children: List<Element> get() = elements

    }
}