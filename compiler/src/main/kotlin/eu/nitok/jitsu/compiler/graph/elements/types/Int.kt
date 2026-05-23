package eu.nitok.jitsu.compiler.graph.elements.types

import eu.nitok.jitsu.common.BitSize
import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.compiler.graph.api.Element
import eu.nitok.jitsu.compiler.graph.api.Type
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*

@Serializable
internal data class Int(override val size: BitSize) : TypeElement(), Type.Int {
    override fun rawType(resolveGeneric: ResolveGenericFn?): TypeElement = this
    override fun toString(): String {
        return "i${size.bits}"
    }

    override fun accepts(type: Type): ReasonedBoolean {
        return if (type is Int && type.size.bits <= this.size.bits) ReasonedBoolean.True(
            "integers accept integers their size and smaller"
        )
        else if (type is UInt && type.size.bits * 2 <= this.size.bits) ReasonedBoolean.True(
            "integers accept unsigned integers that are at most half their size"
        )
        else if (type is Float) ReasonedBoolean.False("Integers only accept integers. To assign non int numbers convert them first")
        else if (type is Int) ReasonedBoolean.False("Integers only accept integers their size or less, since assigning for example a i64 to a i32 can cause number overflow")
        else if (type is UInt) ReasonedBoolean.False("Integers only accept unsigned integers that are at most half their size (you can assign u32 to i64 but not u32 to i32)")
        else ReasonedBoolean.False("$type cannot be assigned to $this")
    }

    @Transient
    override val children: List<Element> = Collections.emptyList()

}