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
    val body: Body
) : Instruction, Element, Accessible<Function>, Accessor, ScopeAware, ScopeProvider, Finalizable {
    override val scope: Scope = Scope(listOf(), mapOf(), mapOf(), parameters.associateBy { it.name.value })

    @Serializable
    sealed interface Body : Element {
        @Serializable
        data class Native(val nativeTarget: String) : Body {
            override val children: List<Element> get() = emptyList()
        }

        /**
         * The body should have been implemented but is not. This is an error that is reported by the parser
         */
        @Serializable
        object Missing : Body {
            override val children: List<Element> get() = emptyList()
        }

        @Serializable
        data class Implementation(val block: CodeBlock) : Body {
            override val children: List<Element> get() = listOf(block)
        }
    }

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

    private lateinit var infoCache: FunctionInfo
    fun info(msg: CompilerMessages): FunctionInfo {
        if (!::infoCache.isInitialized)
            infoCache = calculateFunctionInfo(msg)
        return infoCache
    }

    val signature: Type.FunctionTypeSignature = Type.FunctionTypeSignature(
        returnType,
        parameters.map { Type.FunctionTypeSignature.Parameter(it.name, it.declaredType, it.initialValue != null) }
    )

    override val children: List<Element> get() = listOfNotNull(returnType) + parameters + body

    @Transient
    override val accessToSelf: MutableList<Access<Function>> = mutableListOf()

    override val accessFromSelf: List<Access<*>> by lazy {
        if (body is Body.Implementation) findAccessesFromSelf(body.block.instructions)
        else emptyList()
    }

    @Serializable
    data class Parameter(
        override val name: Located<String>,
        override val declaredType: Type,
        override val initialValue: Expression?
    ) : Variable, Element {
        /**
         * alias for [declaredType]
         */
        val type get() = declaredType
        override val accessToSelf: MutableList<in Access<Variable>> = mutableListOf()
        override val children: List<Element> get() = listOfNotNull(declaredType, initialValue)

        override fun toString(): String {
            return "${name.value}: $declaredType${if (initialValue != null) " = $initialValue" else ""}"
        }

        override val reassignable: Boolean = false
    }

    override fun finalizeGraph(messages: CompilerMessages) {
        infoCache = calculateFunctionInfo(messages)
    }
}

