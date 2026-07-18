package eu.nitok.jitsu.compiler.graph.elements

import eu.nitok.jitsu.common.locating.Locatable
import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.compiler.analysis.FunctionSummaryElement
import eu.nitok.jitsu.compiler.graph.api.*
import eu.nitok.jitsu.compiler.graph.api.Function
import eu.nitok.jitsu.compiler.graph.behaviour.FunctionAware
import eu.nitok.jitsu.compiler.graph.behaviour.ScopeProvider
import eu.nitok.jitsu.compiler.graph.elements.types.FunctionTypeSignature
import eu.nitok.jitsu.compiler.graph.elements.types.TypeElement
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal class FunctionElement(
    override val name: Located<String>?,
    val returnTypeElement: Located<TypeElement>?,
    override val parameters: List<Parameter>,
    val bodyElement: BodyElement,
    override val attributes: List<AttributeElement>,
    val location: Locatable
) : ScopeProvider, Function, InstructionElement, AccessibleElement<Function> {
    override fun getSymbol(module: JitsuModule) = module.getSymbolID(this)
    override var symbolIndex: Int? = null
    override val body: Function.Body = bodyElement.asBody

    @Transient
    override val scope = Scope(variableElements = parameters.associateBy { it.name.value })

    @Transient
    override val returnType = returnTypeElement?.map { it.asType }

    @Serializable
    sealed interface BodyElement {

        val asBody: Function.Body
            get() = when (this) {
                is Native -> this
                is Missing -> this
                is Implementation -> this
            }

        @Serializable
        object Native : BodyElement, Function.Body.Native {
            override val children: List<Element> get() = emptyList()
        }

        /**
         * The body should have been implemented but is not. This is an error that is reported by the parser
         */
        @Serializable
        object Missing : BodyElement, Function.Body.Missing {
            override val children: List<Element> get() = emptyList()
        }

        @Serializable
        class Implementation(internal val codeBlock: CodeBlockElement) : CodeBlock by codeBlock, BodyElement,
            Function.Body.Implementation
    }

    private fun informChildren(children: List<Element>) {
        children.forEach {
            if (it is FunctionAware) it.setEnclosingFunction(this)
            if (it !is Function) informChildren(it.children)
        }
    }

    init {
        informChildren(body.children)
    }

    override var summary: FunctionSummaryElement? = null
        internal set

    @Transient
    override val signature: Type.FunctionTypeSignature = FunctionTypeSignature(
        returnTypeElement?.value,
        parameters.map { FunctionTypeSignature.Parameter(it.name, it.declaredTypeElement, it.initialValue != null) }
    )

    override val children: List<Element> get() = listOfNotNull(returnType?.value) + parameters + body + attributes

    @Transient
    override val accessToSelf: MutableList<Access<Function>> = mutableListOf()

    override val accessFromSelf: List<Access<*>> by lazy {
        if (body is BodyElement.Implementation) findAccessesFromSelf(body.instructions)
        else emptyList()
    }
    private fun findAccessesFromSelf(elems: Iterable<Element>): List<Access<*>> {
        val lst = mutableListOf<Access<*>>()
        fun findAccesses(inst: Element) {
            if (inst is Access<*>) lst.add(inst)
            if (inst !is Accessor) inst.children.forEach(::findAccesses)
        }
        elems.forEach(::findAccesses)
        return lst
    }

    @Transient
    override lateinit var module: JitsuModule


    override fun toString(): String {
        return "Function[${name?.value}$signature]@${hashCode().toString(18)}"
    }


    @Serializable
    internal data class Parameter(
        override val name: Located<String>,
        override val declaredTypeElement: TypeElement,
        override val initialValueElement: ExpressionElement?
    ) : VariableElement, Function.Parameter, AccessibleElement<Variable> {
        override fun getSymbol(module: JitsuModule) = module.getSymbolID(this)
        override var symbolIndex: Int? = null
        @Transient
        override val initialValue: Expression? = initialValueElement?.asExpression

        /**
         * alias for [declaredType]
         */
        override val type get() = declaredType
        override val declaredType: Type get() = declaredTypeElement.asType

        @Transient
        override val accessToSelf: MutableList<Access<Variable>> = mutableListOf()
        override val children: List<Element> get() = listOfNotNull(declaredType, initialValue)

        override fun toString(): String {
            return "${name.value}: $declaredType${if (initialValue != null) " = $initialValue" else ""}"
        }

        override val reassignable: Boolean = false

        @Transient
        override lateinit var module: JitsuModule

    }
}