package eu.nitok.jitsu.compiler.graph.elements

import eu.nitok.jitsu.common.BitSize
import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.common.locating.Location
import eu.nitok.jitsu.compiler.graph.api.Element
import eu.nitok.jitsu.compiler.graph.api.Expression
import eu.nitok.jitsu.compiler.graph.api.Type
import eu.nitok.jitsu.compiler.graph.elements.types.*
import eu.nitok.jitsu.compiler.graph.elements.types.Boolean
import eu.nitok.jitsu.compiler.graph.elements.types.Int
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.Boolean as KBoolean
import kotlin.Int as KInt
import kotlin.UInt as KUInt

@Serializable
internal sealed class ConstantElement<out T> : ExpressionElement {
    @SerialName("resolved_type")
    abstract override val type: Type

    abstract val asConstant: Expression.Constant<T>
    @Contextual
    abstract val value: T
    abstract val literal: String
    @Transient
    override val isConstant: ReasonedBoolean = ReasonedBoolean.True("$value is a constant")


    @Serializable
    internal data class IntConstant(override val value: Long, override val location: Location) : ConstantElement<Long>(),
        Expression.Constant.IntConstant {
        override val asConstant: Expression.Constant<Long> get() = this
        override val typeElement: TypeElement = calculateType(mapOf(), CompilerMessages(), null) ?: Undefined;
        override val type: Type get() = typeElement.asType
        override val literal: String get() = value.toString()
        override fun calculateType(
            context: Map<String, TypeElement>,
            messages: CompilerMessages,
            typeHint: TypeElement?
        ): TypeElement? {
            val minimumType = when (value) {
                in Byte.MIN_VALUE..Byte.MAX_VALUE -> Int(BitSize.BIT_8)
                in Short.MIN_VALUE..Short.MAX_VALUE -> Int(BitSize.BIT_16)
                in KInt.MIN_VALUE..KInt.MAX_VALUE -> Int(BitSize.BIT_32)
                in Long.MIN_VALUE..Long.MAX_VALUE -> Int(BitSize.BIT_64)
                else -> {
                    messages.error(
                        "Int value $value is too large (max value is for i64 is ${Long.MAX_VALUE})",
                        location
                    )
                    return null
                }
            }
            return if (typeHint != null && typeHint.acceptsInstanceOf(minimumType).value) typeHint
            else minimumType
        }

        @Transient
        override val children: List<Element> = listOfNotNull()
    }

    @Serializable
    internal data class UIntConstant(override val value: ULong, override val location: Location) : ConstantElement<ULong>(),
        Expression.Constant.UIntConstant {
        override val asConstant: Expression.Constant<ULong> get() = this
        override val typeElement: TypeElement = calculateType(mapOf(), CompilerMessages()) ?: Undefined
        override val type: Type get() = typeElement.asType
        override val literal: String get() = value.toString()
        override fun calculateType(
            context: Map<String, TypeElement>,
            messages: CompilerMessages,
            typeHint: TypeElement?
        ): TypeElement? {
            val minimumType = when {
                value < 0u -> throw IllegalArgumentException("UInt value $value is negative")
                value <= UByte.MAX_VALUE -> UInt(BitSize.BIT_8)
                value <= UShort.MAX_VALUE -> UInt(BitSize.BIT_16)
                value <= KUInt.MAX_VALUE -> UInt(BitSize.BIT_32)
                value <= ULong.MAX_VALUE -> UInt(BitSize.BIT_64)
                else -> {
                    messages.error("UInt value $value is too large (u64 max value is ${ULong.MAX_VALUE})", location)
                    return null
                }
            }
            return if (typeHint != null && typeHint.acceptsInstanceOf(minimumType).value) typeHint
            else minimumType
        }

        @Transient
        override val children: List<Element> = listOfNotNull()
    }

    @Serializable
    internal data class StringConstant(override val value: String, override val location: Location) : ConstantElement<String>(),
        Expression.Constant.StringConstant {
        override val asConstant: Expression.Constant<String> get() = this
        override val typeElement = TypeReference(Located("String", location), listOf())
        override val type: Type get() = typeElement.asType
        override val literal: String get() = "\"${value}\""

        @Transient
        override val children: List<Element> = listOfNotNull()
        override fun calculateType(context: Map<String, TypeElement>, messages: CompilerMessages, typeHint: TypeElement?): TypeElement {
            return typeElement;
        }
    }

    @Serializable
    class BooleanConstant(override val value: KBoolean, override val location: Location) : ConstantElement<KBoolean>(),
        Expression.Constant.BooleanConstant {
        override val asConstant: Expression.Constant<KBoolean> get() = this
        @Transient override var typeElement: TypeElement = Boolean
        @Transient override val type: Type = Boolean
        override val literal: String get() = value.toString()

        @Transient
        override val children: List<Element> = listOfNotNull()
        override fun calculateType(context: Map<String, TypeElement>, messages: CompilerMessages, typeHint: TypeElement?): TypeElement =
            Boolean
    }
}