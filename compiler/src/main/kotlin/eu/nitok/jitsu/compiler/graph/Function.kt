package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.ast.CompilerMessages
import eu.nitok.jitsu.compiler.ast.Located
import eu.nitok.jitsu.compiler.model.sequence
import kotlinx.serialization.Serializable;
import kotlinx.serialization.Transient

@Serializable
class Function(
    override val name: Located<String>?,
    val returnType: Type?,
    val parameters: List<Parameter>,
    val body: CodeBlock,
    override val scope: Scope
) : Instruction, Element, Accessible<Function>, Accessor, ScopeAware, ScopeProvider {
    constructor(
        name: Located<String>?,
        returnType: Type?,
        parameters: List<Parameter>,
        body: CodeBlock,
        messages: CompilerMessages
    ) : this(
        name, returnType, parameters, body, Scope(
            listOf(), listOf(), listOf(),
            parameters.map { it.asVariable() },
            messages
        )
    )

    init {
        fun informChildren(children: List<Element>) {
            children.forEach {
                if (it is FunctionAware) it.setEnclosingFunction(this)
                if (it is Function) it.informChildren()
                else informChildren(it.children)
            }
        }
        informChildren(body.children)
    }

    override fun setEnclosingScope(parent: Scope) {
        scope.parent = parent
    }

    override val children: List<Element> get() = listOfNotNull(returnType) + parameters + body

    @Transient
    override val accessToSelf: MutableList<Access<Function>> = mutableListOf()

    @Transient
    override val accessFromSelf: List<Access<*>> = findAccessesFromSelf(body.instructions)

    @Serializable
    data class Parameter(
        val name: Located<String>,
        val type: Type,
        val defaultValue: Expression?
    ) : Element {
        fun asVariable(): Variable = Variable(false, name, type, lazy { defaultValue?.implicitType })
        override val children: List<Element> get() = listOfNotNull(type, defaultValue)

        override fun toString(): String {
            return "${name.value}: $type${if (defaultValue != null) " = $defaultValue" else ""}"
        }
    }
}
