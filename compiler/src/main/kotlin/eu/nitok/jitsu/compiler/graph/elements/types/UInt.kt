package eu.nitok.jitsu.compiler.graph.elements.types

import eu.nitok.jitsu.common.BitSize
import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.compiler.graph.api.Element
import eu.nitok.jitsu.compiler.graph.api.Type
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*

@Serializable
internal data class UInt(override val size: BitSize) : TypeElement(), Type.UInt {
    override fun rawType(resolveGeneric: ResolveGenericFn?): TypeElement = this
    override fun toString(): String {
        return "u${size.bits}"
    }

    override fun accepts(type: Type): ReasonedBoolean {
        return if (type is UInt && type.size.bits <= this.size.bits) ReasonedBoolean.True(
            "unsigned integers accept unsigned integers their size and smaller"
        )
        else if (type is UInt) ReasonedBoolean.False("$type is too large to fit into a $this")
        else if (type is Int || type is Float) ReasonedBoolean.False("Unsigned integers only accept unsigned integers. To assign non uint numbers convert them first")
        else ReasonedBoolean.False("$type cannot be assigned to $this")
    }

    @Transient
    override val children: List<Element> = Collections.emptyList()

}