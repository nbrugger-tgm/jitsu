package eu.nitok.jitsu.compiler.graph.elements.types

import eu.nitok.jitsu.common.BitSize
import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.compiler.graph.api.Element
import eu.nitok.jitsu.compiler.graph.api.Type
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*

@Serializable
internal data class Float(override val size: BitSize = BitSize.BIT_32) : TypeElement(), Type.Float {
    override fun rawType(resolveGeneric: ResolveGenericFn?): TypeElement = this
    override fun toString(): String {
        return "f${size.bits}"
    }

    override fun accepts(type: Type): ReasonedBoolean {
        if (type is Float && type.size.bits <= this.size.bits) return ReasonedBoolean.True(
            "floats accept floats their size and smaller"
        )
        if (type is Float) return ReasonedBoolean.False("Floats only accept floats their size or smaller")
        return ReasonedBoolean.False("$type cannot be assigned to $this")
    }

    @Transient
    override val children: List<Element> = Collections.emptyList()

}