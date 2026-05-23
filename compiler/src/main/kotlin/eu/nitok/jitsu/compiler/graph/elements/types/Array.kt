package eu.nitok.jitsu.compiler.graph.elements.types

import eu.nitok.jitsu.common.BitSize
import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.compiler.graph.api.Element
import eu.nitok.jitsu.compiler.graph.api.Type
import eu.nitok.jitsu.compiler.graph.elements.ConstantElement
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal data class Array(
    val elementTypeElement: TypeElement,
    override val size: ConstantElement.IntConstant?
) : TypeElement(), Type.Array{
    override val elementType get() = elementTypeElement.asType
    override val sizeType: Type
        get() = size?.type ?: Int(BitSize.BIT_64)

    override fun rawType(resolveGeneric: ResolveGenericFn?): TypeElement {
        return Array(elementTypeElement.rawType(resolveGeneric), size)
    }

    override fun accepts(type: Type): ReasonedBoolean {
        if (type !is Type.Array) return ReasonedBoolean.False("$type is not an array and can therefore not be assigned to an array")
        val elementsAccept = this.elementTypeElement.acceptsInstanceOf(type.elementType)
        return if (elementsAccept.value) {
            if (this.size != null && type.size != this.size) {
                ReasonedBoolean.False("$this and $type have differing fixed sizes")
            } else ReasonedBoolean.True("$type is an array with an assignable element type", elementsAccept)
        } else {
            ReasonedBoolean.False(
                "Element type of $type (${type.elementType}) is not compatible with ${this.elementTypeElement}",
                elementsAccept
            )
        }
    }

    @Transient
    override val children: List<Element> = listOfNotNull(elementTypeElement)

    override fun toString(): String {
        return "$elementTypeElement[${size?.toString() ?: ""}]"
    }

}