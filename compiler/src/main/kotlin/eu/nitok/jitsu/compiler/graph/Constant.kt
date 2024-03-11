package eu.nitok.jitsu.compiler.graph


import eu.nitok.jitsu.compiler.ast.Located
import eu.nitok.jitsu.compiler.model.BitSize
import eu.nitok.jitsu.compiler.parser.Range
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.lang.IllegalArgumentException

@Serializable
sealed class Constant<out T> : Expression, Element {
    @SerialName("resolved_type")
    abstract val type: Type
    abstract val literal: String
    abstract val originLocation: Range
    @Transient override val isConstant: ReasonedBoolean = ReasonedBoolean.True

    @Contextual
    abstract val value: T


    @Serializable
    data class IntConstant(override val value: Long, val explicitType: Type.Int? = null, override val originLocation: Range) : Constant<Long>() {
        override val type: Type.Int
            get() = explicitType ?: when (value) {
                in Byte.MIN_VALUE..Byte.MAX_VALUE -> Type.Int(BitSize.BIT_8)
                in Short.MIN_VALUE..Short.MAX_VALUE -> Type.Int(BitSize.BIT_16)
                in Int.MIN_VALUE..Int.MAX_VALUE -> Type.Int(BitSize.BIT_32)
                in Long.MIN_VALUE..Long.MAX_VALUE -> Type.Int(BitSize.BIT_64)
                else -> throw IllegalStateException("Int value $value is too large")
            };
        override val literal: String get() = value.toString()
        @Transient override val children: List<Element> = listOfNotNull(explicitType)
    }

    @Serializable
    data class UIntConstant(override val value: ULong, val explicitType: Type.UInt? = null, override val originLocation: Range) : Constant<ULong>() {
        override val type: Type.UInt = explicitType ?: run {
            when {
                value < 0u -> throw IllegalArgumentException("UInt value $value is negative")
                value <= UByte.MAX_VALUE -> Type.UInt(BitSize.BIT_8)
                value <= UShort.MAX_VALUE -> Type.UInt(BitSize.BIT_16)
                value <= UInt.MAX_VALUE -> Type.UInt(BitSize.BIT_32)
                value <= ULong.MAX_VALUE -> Type.UInt(BitSize.BIT_64)
                else -> throw IllegalArgumentException("UInt value $value is too large")
            }
        };
        override val literal: String get() = value.toString()
        @Transient override val children: List<Element> = listOfNotNull(explicitType)
    }

    @Serializable
    data class StringConstant(override val value: String, override val originLocation: Range) : Constant<String>() {
        override val type: Type = Type.TypeReference(Located(value, originLocation), mapOf())
        override val literal: String get() = "\"${value}\""
        @Transient override val children: List<Element> = listOfNotNull()
    }

    class BooleanConstant(override val value: Boolean, override val originLocation: Range) : Constant<Boolean>() {
        override val type: Type get() = Type.Boolean
        override val literal: String get() = value.toString()
        @Transient override val children: List<Element> = listOfNotNull()
    }
}