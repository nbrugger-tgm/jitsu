package eu.nitok.jitsu.compiler.graph.elements.types

import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.compiler.graph.api.Element
import eu.nitok.jitsu.compiler.graph.api.Type
import kotlinx.serialization.Serializable

@Serializable
internal class Union(val optionTypeElements: List<TypeElement>) : TypeElement(), Type.Union {
    override var options: List<Type> = optionTypeElements.map { it.asType }
    override fun rawType(resolveGeneric: ResolveGenericFn?): TypeElement {
        val resolvedOptions = optionTypeElements.map { it.rawType(resolveGeneric) }
            .flatMap { type -> if (type is Union) type.optionTypeElements else listOf(type) }
            .distinct()
        return if (resolvedOptions.size > 1) Union(resolvedOptions)
        else resolvedOptions.single()
    }

    override fun accepts(type: Type): ReasonedBoolean {
        val optionsAccept = options.map { it.toString() to it.acceptsInstanceOf(type) }
        val matches = optionsAccept.filter { it.second.value }
        if (matches.isNotEmpty()) return ReasonedBoolean.True(
            "$type is assignable to one or more options of $this",
            causes = matches
        )
        return ReasonedBoolean.False(
            "$type is not assignable to any of: ${options.joinToString(", ")}",
            causes = optionsAccept
        )
    }

    override val children: List<Element> get() = options
    override fun toString(): String {
        return options.joinToString(" | ")
    }
}