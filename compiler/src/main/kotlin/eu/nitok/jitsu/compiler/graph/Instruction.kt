package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.ast.CompilerMessages
import eu.nitok.jitsu.compiler.ast.Located
import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage
import eu.nitok.jitsu.compiler.parser.Range
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
    data class VariableDeclaration(val variable: Variable, val value: Expression?) : Instruction {
        override val children: List<Element> get() = listOfNotNull(variable, value)
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

        override fun calculateType(context: Map<String, Type>): Type? {
            return target?.returnType
        }

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
    }
}