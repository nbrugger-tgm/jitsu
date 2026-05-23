package eu.nitok.jitsu.compiler.graph.elements.types

import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.compiler.graph.api.Element
import eu.nitok.jitsu.compiler.graph.api.Type
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*

@Serializable
internal data object Null : TypeElement(), Type.Null {
    override fun rawType(resolveGeneric: ResolveGenericFn?): TypeElement = this
    override fun toString(): String {
        return "null"
    }

    override fun accepts(type: Type): ReasonedBoolean {
        return if (type is Null) ReasonedBoolean.True("null is assignable to the null type")
        else ReasonedBoolean.False("null is the only value assignable to the null type")
    }

    @Transient
    override val children: List<Element> = Collections.emptyList()

}