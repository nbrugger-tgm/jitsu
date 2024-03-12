package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.ast.CompilerMessages
import eu.nitok.jitsu.compiler.ast.Located
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed interface Instruction : Element {
    @Serializable
    data class Return(val value: Expression?) : Instruction, FunctionAware {
        override val children: List<Element> get() = listOfNotNull(value)

        @Transient
        lateinit var function: Function
        override fun setEnclosingFunction(parent: Function) {
            function = parent
        }
    }

    @Serializable
    data class VariableDeclaration(val variable: Variable, val value: Expression) : Instruction {
        override val children: List<Element> get() = listOf(variable, value)
    }

    @Serializable
    data class FunctionCall(override val reference: Located<String>, private val callParameters: List<Expression>) :
        Instruction,
        ScopeAware,
        Expression,
        Access.FunctionAccess {
        val parameters: Map<String, Expression>
            get() = callParameters
                .mapIndexed { index, parameter -> parameter to target?.parameters?.get(index)?.name?.value }
                .filter { it.second != null }
                .associateBy({ it.second!! }, { it.first })
        override val isConstant: ReasonedBoolean
            get() = ReasonedBoolean.False("Function call constant analysis not implemented yet")
        override val implicitType: Type?
            get() = Type.FunctionTypeSignature(
                target?.returnType,
                target?.parameters?.map { Type.FunctionTypeSignature.Parameter(it.name, it.type) } ?: listOf()
            )
        override val children: List<Element> get() = callParameters.toList()

        @Transient
        override lateinit var accessor: Accessor

        @Transient
        override var target: Function? = null

        @Transient
        lateinit var scope: Scope

        override fun resolve(messages: CompilerMessages) {
            target = scope.resolveFunction(reference, messages)
        }

        override fun setEnclosingScope(parent: Scope) {
            scope = parent
        }
    }
}