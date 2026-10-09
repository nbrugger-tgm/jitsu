package eu.nitok.jitsu.compiler.graph.elements.types

import eu.nitok.jitsu.common.CompilerMessage
import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.compiler.graph.api.Element
import eu.nitok.jitsu.compiler.graph.api.Type
import eu.nitok.jitsu.compiler.graph.behaviour.Resolvable
import eu.nitok.jitsu.compiler.graph.behaviour.ScopeAware
import eu.nitok.jitsu.compiler.graph.elements.AccessElement
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Reference to a type by name only (`Foo`), without type parameters.
 *
 * The target may be any type definition, including a [ParameterizedTypeElement] as long as it declares no type parameters.
 * References with type parameters (`Foo<A>`) are [ParameterizedTypeReference]s.
 */
@Serializable
internal class TypeReference private constructor(
    //This is a workaround for https://youtrack.jetbrains.com/issue/KT-83
    private val access: AccessElement.TypeAccessElement
) : TypeElement(), AccessElement.TypeAccess by access, Type.TypeReference, ScopeAware, Resolvable {

    /*
     * Noone should ever touch this besides GraphBuilder
     */
    constructor(reference: Located<String>) : this(AccessElement.TypeAccessElement(reference))

    override fun rawType(resolveGeneric: ResolveGenericFn?): TypeElement {
        return when (val target = targetElement ?: return Undefined) {
            is ParameterizedTypeElement ->
                if (target.typeParameters.isEmpty()) target.toType(emptyMap())
                else Undefined // reported in resolve()
            //TODO: this could be rawTypeElement, but depending on order it might not yet be set
            // A on demand setup would provide better performance, but 1. there are not "messages" at "on demand"
            // and 2. errors in the rawType progress wouldn't arise unless the raw type is used
            is TypeParameterElement -> target.rawType(resolveGeneric)
            is DirectTypeDefinitionElement -> target.rawTypeElement
        }
    }

    override fun accepts(type: Type): ReasonedBoolean {
        return rawType.acceptsInstanceOf(type)
    }

    override fun resolve(messages: CompilerMessages) {
        access.resolve(messages)
        validateTarget(messages)
    }

    override fun restore(messages: CompilerMessages) {
        access.restore(messages)
    }

    private fun validateTarget(messages: CompilerMessages) {
        val target = targetElement as? ParameterizedTypeElement ?: return
        if (target.typeParameters.isEmpty()) return
        messages.error(
            "Type '${reference.value}' requires ${target.typeParameters.size} type parameter(s)",
            reference.location,
            CompilerMessage.Hint(
                "Type parameters of '${reference.value}' are defined here",
                target.typeParameters.first().name.location.rangeTo(target.typeParameters.last().name.location)
            )
        )
    }

    @Transient
    override val children: List<Element> = emptyList()

    override fun toString(): String = reference.value
}

/**
 * Reference to a [parameterized type][ParameterizedTypeElement] with type parameters, e.g. `List<i32>`
 */
@Serializable
internal class ParameterizedTypeReference private constructor(
    val typeParameterElements: List<Located<TypeElement>>,
    //This is a workaround for https://youtrack.jetbrains.com/issue/KT-83
    private val access: AccessElement.ParameterizedTypeAccessElement
) : TypeElement(), AccessElement.ParameterizedTypeAccess by access, Type.ParameterizedTypeReference, ScopeAware,
    Resolvable {

    /*
     * Noone should ever touch this besides GraphBuilder
     */
    constructor(reference: Located<String>, typeParameters: List<Located<TypeElement>>) : this(
        typeParameters,
        AccessElement.ParameterizedTypeAccessElement(reference)
    )

    override val typeParameters: List<Located<Type>> by lazy {
        typeParameterElements.map { it.map { type -> type.asType } }
    }

    /**
     * TODO: specialization is not implemented yet
     */
    override val specializedTarget: Type.SpecializedType? get() = null

    /**
     * Mapping of the targets type parameter names to the types given by this reference.
     * Null if not resolved or the type parameters do not match the target.
     */
    @Transient
    var targetMappedTypeParameters: Map<String, TypeElement>? = null
        private set

    override fun rawType(resolveGeneric: ResolveGenericFn?): TypeElement {
        val target = targetElement ?: return Undefined
        val mapping = targetMappedTypeParameters ?: return Undefined
        return target.toType(mapping)
    }

    private fun mapTypeParametersToTarget(
        target: ParameterizedTypeElement,
        messages: CompilerMessages
    ): Map<String, TypeElement>? {
        val definitionHint = if (target.typeParameters.isNotEmpty())
            CompilerMessage.Hint(
                "Type parameters of target type are defined here",
                target.typeParameters.first().name.location.rangeTo(target.typeParameters.last().name.location)
            )
        else
            CompilerMessage.Hint("Target type has no type parameters defined", target.name.location)

        typeParameterElements.drop(target.typeParameters.size).forEachIndexed { overflowIndex, type ->
            val index = target.typeParameters.size + overflowIndex
            messages.error(
                "Type parameter ${type.value} ($index) does not exist in the definition of $target",
                type.location,
                definitionHint
            )
        }
        val missing = target.typeParameters.drop(typeParameterElements.size)
        if (missing.isNotEmpty()) {
            messages.error(
                "Missing type parameter(s) ${missing.joinToString { it.name.value }} for $target",
                reference.location,
                definitionHint
            )
        }
        if (missing.isNotEmpty() || typeParameterElements.size > target.typeParameters.size) return null
        return target.typeParameters.zip(typeParameterElements)
            .associate { (parameter, type) -> parameter.name.value to type.value }
    }

    override fun accepts(type: Type): ReasonedBoolean {
        return rawType.acceptsInstanceOf(type)
    }

    override fun resolve(messages: CompilerMessages) {
        access.resolve(messages)
        recalculateTypeParameterMapping(messages)
    }

    override fun restore(messages: CompilerMessages) {
        access.restore(messages)
        recalculateTypeParameterMapping(messages)
    }

    private fun recalculateTypeParameterMapping(messages: CompilerMessages) {
        val target = targetElement ?: return
        targetMappedTypeParameters = mapTypeParametersToTarget(target, messages)
    }

    @Transient
    override val children: List<Element> = typeParameterElements.map { it.value }

    override fun toString(): String {
        return reference.value + typeParameterElements.joinToString(
            prefix = "<",
            postfix = ">",
            separator = ", "
        ) { it.value.toString() }
    }
}
