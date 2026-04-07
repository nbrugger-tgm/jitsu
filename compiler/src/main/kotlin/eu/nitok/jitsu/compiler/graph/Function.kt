package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.analysis.FunctionSummary
import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.Located
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Function(
    override val name: Located<String>?,
    val returnType: Located<Type>?,
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

    @Transient
    var summary: FunctionSummary? = null
        internal set
    val signature: Type.FunctionTypeSignature = Type.FunctionTypeSignature(
        returnType?.value,
        parameters.map { Type.FunctionTypeSignature.Parameter(it.name, it.declaredType, it.initialValue != null) }
    )

    override val children: List<Element> get() = listOfNotNull(returnType?.value) + parameters + body

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
        override val accessToSelf: MutableList<Access<Variable>> = mutableListOf()
        override val children: List<Element> get() = listOfNotNull(declaredType, initialValue)

        override fun toString(): String {
            return "${name.value}: $declaredType${if (initialValue != null) " = $initialValue" else ""}"
        }

        override val reassignable: Boolean = false
    }

    override fun finalizeGraph(messages: CompilerMessages) {
        // no-op: analysis moved to AnalysisRepository
    }

    override fun toString(): String {
        return "Function[${name?.value}$signature]@${hashCode().toString(18)}"
    }
}

