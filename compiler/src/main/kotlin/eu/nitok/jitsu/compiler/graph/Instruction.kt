package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.common.ReasonedBoolean

import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.Located
import eu.nitok.jitsu.common.Range
import eu.nitok.jitsu.common.locatedAt
import eu.nitok.jitsu.compiler.analysis.FunctionSignatureMatch
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.math.min

@Serializable
sealed interface Instruction : Element {
    @Serializable
    data class Return(val value: Expression?, val location: Range) : Instruction, FunctionAware {
        override val children: List<Element> get() = listOfNotNull(value)

        @Transient
        lateinit var function: Function
        override fun setEnclosingFunction(parent: Function) {
            function = parent
        }
    }

    @Serializable
    data class FunctionCall(
        override val reference: Located<String>,
        val callParameters: List<Expression>,
        override val location: Range
    ) :
        Instruction,
        ScopeAware,
        Expression,
        Access.FunctionAccess {
        val parameters: Map<String, Expression>
            get() {
                return callParameters.subList(0, min(target?.parameters?.size ?: 0, callParameters.size))
                    .mapIndexed { index, parameter -> parameter to target?.parameters?.get(index)?.name?.value }
                    .filter { it.second != null }
                    .associateBy({ it.second!! }, { it.first })
                //todo add default values as parameters
            }
        override val isConstant: ReasonedBoolean
            get() = ReasonedBoolean.False("Function call constant analysis not implemented yet")

        override fun calculateType(context: Map<String, Type>, messages: CompilerMessages, typeHint: Type?): Type? {
            if(target == null) {
                target = resolveTarget(context, messages)
                target?.accessToSelf?.add(this)
            }
            val type = target?.returnType?.value
            if(type != null) this.type = type
            return type
        }

        fun resolveTarget(context: Map<String, Type>, messages: CompilerMessages): Function? {
            var target: Function?
            val initialTypecalcMessages = CompilerMessages()
            val callSiteTypes = callParameters.map {
                (it.calculateType(context, initialTypecalcMessages) ?: Type.Undefined).locatedAt(it.location)
            }.toTypedArray()
            target = scope.resolveFunction(
                reference,
                callSiteTypes,
                messages
            )
            if (target != null && callSiteTypes.any { it.value is Type.Undefined } || initialTypecalcMessages.errors.isNotEmpty()) {
                //try again with typehints and hope it does not produce undefined / compiler messages
                callParameters.forEachIndexed { index, expression ->
                    expression.calculateType(
                        context,
                        messages,
                        target?.parameters?.getOrNull(index)?.type
                    )
                }
            } else {
                messages.add(initialTypecalcMessages)
            }
            return target
        }

        override lateinit var type: Type
            private set;

        override val children: List<Element> get() = callParameters.toList()

        @Transient
        override lateinit var accessor: Accessor

        @Transient
        override var target: Function? = null

        @Transient
        lateinit var scope: Scope

        override fun resolveAccessTarget(messages: CompilerMessages): Function? {
            //for a function to resolve it needs its children to be resolved
            //because the types of the call-parameters are needed to resolve the correct overload
            return target
        }

        override fun setEnclosingScope(parent: Scope) {
            scope = parent
        }

        override fun toString(): String {
            return "${reference.value}(${callParameters.joinToString(", ")})"
        }
    }
}