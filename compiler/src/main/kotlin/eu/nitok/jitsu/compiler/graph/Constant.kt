package eu.nitok.jitsu.compiler.graph


import eu.nitok.jitsu.compiler.model.BitSize
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.lang.IllegalArgumentException
import java.math.BigDecimal

@Serializable
sealed class Constant<out T> : Expression {
    @SerialName("resolved_type")
    abstract val type: ResolvedType
    abstract val literal: String
    abstract val originLocation: Location

    @Contextual
    abstract val value: T

    @Serializable
    data class IntConstant(override val value: Long, val explicitType: ResolvedType.Int? = null, override val originLocation: Location) : Constant<Long>() {
        override val type: ResolvedType.Int
            get() = explicitType ?: when (value) {
                in Byte.MIN_VALUE..Byte.MAX_VALUE -> ResolvedType.Int(BitSize.BIT_8)
                in Short.MIN_VALUE..Short.MAX_VALUE -> ResolvedType.Int(BitSize.BIT_16)
                in Int.MIN_VALUE..Int.MAX_VALUE -> ResolvedType.Int(BitSize.BIT_32)
                in Long.MIN_VALUE..Long.MAX_VALUE -> ResolvedType.Int(BitSize.BIT_64)
                else -> throw IllegalStateException("Int value $value is too large")
            };
        override val literal: String get() = value.toString()
    }

    @Serializable
    data class UIntConstant(override val value: ULong, val explicitType: ResolvedType.UInt? = null, override val originLocation: Location) : Constant<ULong>() {
        override val type: ResolvedType.UInt = explicitType ?: run {
            when {
                value < 0u -> throw IllegalArgumentException("UInt value $value is negative")
                value <= UByte.MAX_VALUE -> ResolvedType.UInt(BitSize.BIT_8)
                value <= UShort.MAX_VALUE -> ResolvedType.UInt(BitSize.BIT_16)
                value <= UInt.MAX_VALUE -> ResolvedType.UInt(BitSize.BIT_32)
                value <= ULong.MAX_VALUE -> ResolvedType.UInt(BitSize.BIT_64)
                else -> throw IllegalArgumentException("UInt value $value is too large")
            }
        };
        override val literal: String get() = value.toString()
    }

    @Serializable
    data class StringConstant(override val value: String, override val originLocation: Location) : Constant<String>() {
        override val type: ResolvedType = ResolvedType.String
        override val literal: String get() = "\"${value}\""
    }

    class BooleanConstant(override val value: Boolean, override val originLocation: Location) : Constant<Boolean>() {
        override val type: ResolvedType get() = ResolvedType.Boolean()
        override val literal: String get() = value.toString()

    }
}