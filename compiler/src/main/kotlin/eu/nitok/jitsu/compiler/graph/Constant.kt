package eu.nitok.jitsu.compiler.graph


import eu.nitok.jitsu.compiler.ast.Located
import eu.nitok.jitsu.compiler.model.BitSize
import eu.nitok.jitsu.compiler.parser.Range
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed class Constant<out T> : Expression, Element {
    @SerialName("resolved_type")
    abstract val type: Type
    abstract val literal: String
    @Transient override val isConstant: ReasonedBoolean = ReasonedBoolean.True("$value is a constant", listOf() ,listOf())

    @Contextual
    abstract val value: T


    @Serializable
    data class IntConstant(override val value: Long, override val location: Range) : Constant<Long>() {
        override val type: Type get() = calculateType(mapOf());
        override val literal: String get() = value.toString()
        override fun calculateType(context: Map<String, Type>): Type {
            return when (value) {
                in Byte.MIN_VALUE..Byte.MAX_VALUE -> Type.Int(BitSize.BIT_8)
                in Short.MIN_VALUE..Short.MAX_VALUE -> Type.Int(BitSize.BIT_16)
                in Int.MIN_VALUE..Int.MAX_VALUE -> Type.Int(BitSize.BIT_32)
                in Long.MIN_VALUE..Long.MAX_VALUE -> Type.Int(BitSize.BIT_64)
                else -> throw IllegalStateException("Int value $value is too large (max value is for i64 is ${Long.MAX_VALUE})")
            }
        }
        @Transient override val children: List<Element> = listOfNotNull()
    }

    @Serializable
    data class UIntConstant(override val value: ULong, override val location: Range) : Constant<ULong>() {
        override val type: Type.UInt = calculateType(mapOf());
        override val literal: String get() = value.toString()
        override fun calculateType(context: Map<String, Type>): Type.UInt {
            return when {
                value < 0u -> throw IllegalArgumentException("UInt value $value is negative")
                value <= UByte.MAX_VALUE -> Type.UInt(BitSize.BIT_8)
                value <= UShort.MAX_VALUE -> Type.UInt(BitSize.BIT_16)
                value <= UInt.MAX_VALUE -> Type.UInt(BitSize.BIT_32)
                value <= ULong.MAX_VALUE -> Type.UInt(BitSize.BIT_64)
                else -> throw IllegalArgumentException("UInt value $value is too large (u64 max value is ${ULong.MAX_VALUE})")
            }
        }

        @Transient override val children: List<Element> = listOfNotNull()
    }

    @Serializable
    data class StringConstant(override val value: String, override val location: Range) : Constant<String>() {
        override val type: Type = Type.TypeReference(Located("String", location), listOf())
        override val literal: String get() = "\"${value}\""
        @Transient override val children: List<Element> = listOfNotNull()
        override fun calculateType(context: Map<String, Type>): Type? {
           return type;
        }
    }

    class BooleanConstant(override val value: Boolean, override val location: Range) : Constant<Boolean>() {
        override val type: Type get() = Type.Boolean
        override val literal: String get() = value.toString()
        @Transient override val children: List<Element> = listOfNotNull()
        override fun calculateType(context: Map<String, Type>): Type = Type.Boolean
    }
}