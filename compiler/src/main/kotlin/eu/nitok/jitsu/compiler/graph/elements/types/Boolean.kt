package eu.nitok.jitsu.compiler.graph.elements.types

import eu.nitok.jitsu.common.BitSize
import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.compiler.graph.api.Element
import eu.nitok.jitsu.compiler.graph.api.Type
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*

@Serializable
internal data object Boolean : TypeElement(), Type.Boolean {
    override fun toString(): String {
        return "boolean"
    }

    override fun rawType(resolveGeneric: ResolveGenericFn?): TypeElement = this

    override fun accepts(type: Type): ReasonedBoolean {
        return if (type is Boolean) {
            ReasonedBoolean.True("boolean is assignable to boolean")
        } else {
            ReasonedBoolean.False("only boolean is assignable to boolean")
        }
    }

    @Transient
    override val children: List<Element> = Collections.emptyList()
    override val size: BitSize
        get() = BitSize.BIT_1
}