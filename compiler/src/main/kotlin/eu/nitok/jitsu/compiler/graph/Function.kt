package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.analysis.FunctionInfo
import eu.nitok.jitsu.compiler.analysis.calculateFunctionInfo
import eu.nitok.jitsu.compiler.ast.CompilerMessages
import eu.nitok.jitsu.compiler.ast.Located
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Function(
    override val name: Located<String>?,
    val returnType: Type?,
    val parameters: List<Parameter>,
    val body: CodeBlock,
    override val scope: Scope
) : Instruction, Element, Accessible<Function>, Accessor, ScopeAware, ScopeProvider, Finalizable {
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
                if (it is Function) it.setScopes()
                else informChildren(it.children)
            }
        }
        informChildren(body.children)
    }

    override fun setEnclosingScope(parent: Scope) {
        scope.parent = parent
    }

    @Transient
    private lateinit var infoCache: FunctionInfo
    fun info(msg: CompilerMessages): FunctionInfo {
        if(!::infoCache.isInitialized)
            infoCache = calculateFunctionInfo(msg)
        return infoCache
    }
    val signature: Type.FunctionTypeSignature = Type.FunctionTypeSignature(
        returnType,
        parameters.map { Type.FunctionTypeSignature.Parameter(it.name, it.type, it.defaultValue != null) }
    )

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
        fun asVariable(): Variable = Variable(false, name, type, defaultValue)
        override val children: List<Element> get() = listOfNotNull(type, defaultValue)

        override fun toString(): String {
            return "${name.value}: $type${if (defaultValue != null) " = $defaultValue" else ""}"
        }
    }

    override fun finalizeGraph(messages: CompilerMessages) {
        infoCache = calculateFunctionInfo(messages)
    }
}

