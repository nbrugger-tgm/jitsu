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

@Serializable
internal class TypeReference private constructor(
    val genericParameterElements: List<Located<TypeElement>>,
    //This is a workaround for https://youtrack.jetbrains.com/issue/KT-83
    private val access: AccessElement.TypeAccessElement
) : TypeElement(), AccessElement.TypeAccess by access, Type.TypeReference, ScopeAware, Resolvable {
    override val genericParameters: List<Located<Type>> by lazy {
        genericParameterElements.map { it.map { it.asType } }
    }

    @Transient
    final var targetMappedGenerics: Map<String, TypeElement>? = null
        private set


    /*
     * Noone should ever touch this besides GraphBuilder
     */
    constructor(reference: Located<String>, genericParameters: List<Located<TypeElement>>) : this(
        genericParameters,
        AccessElement.TypeAccessElement(reference)
    )

    override fun rawType(resolveGeneric: ResolveGenericFn?): TypeElement {
        val target = targetElement ?: return Undefined
        val raw = when (target) {
            is ParameterizedTypeElement -> resolveParameterized(target)
            //TODO: this could be rawTypeElement, but depending on order it might not yet be set
            // A on demand setup would provide better performance, but 1. there are not "messages" at "on demand"
            // and 2. errors in the rawType progress wouldn't arise unless the raw type is used
            is TypeParameterElement -> target.rawType(resolveGeneric)
            is DirectTypeDefinitionElement -> target.rawTypeElement
        }
        return raw
    }

    private fun resolveParameterized(target: ParameterizedTypeElement): TypeElement {
        return target.toType(
            targetMappedGenerics
                ?: throw IllegalStateException("targetMappedGenerics should have been set in resolve() by the time rawType is called")
        )
    }

    private fun mapGenericsToTarget(
        target: ParameterizedTypeElement,
        messages: CompilerMessages
    ): Map<String, TypeElement> = genericParameterElements.mapIndexedNotNull { index, type ->
        val resolved = type.value
        val targetGenericName = target.generics.getOrNull(index)?.name?.value
        if (targetGenericName == null) {
            messages.error(
                "Generic parameter $resolved ($index) does not exist in the definition of $target",
                type.location,
                if (!target.generics.isEmpty())
                    CompilerMessage.Hint(
                        "Generics of target type are defined here",
                        target.generics.first().name.location.rangeTo(target.generics.last().name.location)
                    )
                else
                    CompilerMessage.Hint(
                        "Target type has no generics defined",
                        target.name.location
                    )
            )
            null
        } else resolved to targetGenericName
    }.associateBy({ it.second }, { it.first })

    override fun accepts(type: Type): ReasonedBoolean {
        return rawType.acceptsInstanceOf(type)
    }

    override fun resolve(messages: CompilerMessages) {
        access.resolve(messages)
        recalculateGenericMappingForTarget(messages)
    }

    private fun recalculateGenericMappingForTarget(messages: CompilerMessages) {
        val target = targetElement
        if (target is ParameterizedTypeElement)
            targetMappedGenerics = mapGenericsToTarget(target, messages)
    }

    override fun restore(messages: CompilerMessages) {
        access.restore(messages)
        recalculateGenericMappingForTarget(messages)
    }

    @Transient
    override val children: List<Element> = genericParameters.map { it.value }

    override fun toString(): String {
        return reference.value + if (!genericParameters.isEmpty()) genericParameters.joinToString(
            prefix = "<",
            postfix = ">",
            separator = ", "
        ) { it.value.toString() } else ""
    }
}