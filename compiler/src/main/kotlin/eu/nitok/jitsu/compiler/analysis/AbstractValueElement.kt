package eu.nitok.jitsu.compiler.analysis

import eu.nitok.jitsu.compiler.graph.api.Type
import eu.nitok.jitsu.compiler.graph.api.analysis.AbstractValue
import eu.nitok.jitsu.compiler.graph.elements.types.TypeElement
import kotlinx.serialization.Serializable

/**
 * Abstract representation of a compile-time value.
 * Forms a three-element lattice: NoValue ⊑ Const(c) ⊑ Unknown
 */
@Serializable
internal sealed interface AbstractValueElement {
    val asAbstractValue: AbstractValue
        get() = when(this) {
        is AbstractValue -> this
        is Const -> this
        NoValue -> NoValue
        Unknown -> Unknown
    }
    /** No value observed (unreachable code path, or no return). */
    @Serializable
    data object NoValue : AbstractValue.NoValue, AbstractValueElement {
        override fun toString(): String = "NoValue"
    }

    /** A known compile-time constant value. Stored as string for serialization. */
    @Serializable
    data class Const(override val value: String,  val valueTypeElement: TypeElement) : AbstractValue.Const , AbstractValueElement{
        override fun toString(): String = "Const($value: $valueType)"
        override val valueType: Type get() = valueTypeElement.asType
    }

    /** Value exists but is not compile-time determinable. */
    @Serializable
    data object Unknown : AbstractValue.Unknown , AbstractValueElement{
        override fun toString(): String = "Unknown"
    }

    /**
     * Lattice join: combines two abstract values.
     * - NoValue is the bottom element (absorbs into the other).
     * - Same Const stays Const.
     * - Different Consts or any Unknown produces Unknown.
     */
    fun join(other: AbstractValueElement): AbstractValueElement = when {
        this is NoValue -> other
        other is NoValue -> this
        this is Const && other is Const && this == other -> this
        else -> Unknown
    }
}