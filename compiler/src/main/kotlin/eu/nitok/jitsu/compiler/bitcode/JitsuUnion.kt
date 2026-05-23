package eu.nitok.jitsu.compiler.bitcode

import eu.nitok.jitsu.common.BitSize
import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.compiler.bitcode.LowLevelType.LLStruct
import eu.nitok.jitsu.compiler.bitcode.LowLevelType.LLUnion
import eu.nitok.jitsu.compiler.graph.api.Element
import eu.nitok.jitsu.compiler.graph.api.Type

/**
 * Jitsu union type - a higher-level abstraction combining LLStruct and LLUnion.
 *
 * Layout: { option: i32, value: union { o0: T0, o1: T1, ... } }
 * - option: discriminant tag indicating which variant is active
 * - value: C-style union of all option types (overlapping memory)
 *
 * This class provides typed access methods for tagged union operations.
 */
class JitsuUnion private constructor(
    val options: List<LowLevelType>,
    val layout: LLStruct,
    override val graphType: Type.Union
) : LowLevelType.Custom(layout) {
    private val valueUnion = layout.fieldType("value") as LLUnion

    /**
     * Access the option index (discriminant tag) field.
     */
    fun optionIndex(union: LowLevelExpression.Field): LowLevelExpression.Read {
        return layout.accessField(union, "option")
    }

    /**
     * Access the value union (contains o0, o1, ... members).
     */
    fun valueField(union: LowLevelExpression.Field): LowLevelExpression.Read {
        return layout.accessField(union, "value")
    }

    /**
     * Access a specific option's value by index.
     */
    fun option(union: LowLevelExpression.Field, index: Int): LowLevelExpression.Read {
        require(index in options.indices) { "Option index $index out of range [0, ${options.size})" }
        val value = valueField(union)
        return valueUnion.accessMember(value, "o$index")
    }

    /**
     * Get the type of a specific option.
     */
    fun optionType(index: Int): LowLevelType {
        require(index in options.indices) { "Option index $index out of range [0, ${options.size})" }
        return options[index]
    }

    /**
     * Find the index of an option type that semantically accepts the given graph type.
     * Returns null if no matching option is found.
     */
    fun findOptionIndex(graphType: Type): Int? {
        val exactMatch = { options.indexOfFirst { it.graphType == graphType }.takeIf { it >= 0 } }
        val assignableMatch =
            { options.indexOfFirst { it.graphType.acceptsInstanceOf(graphType).value }.takeIf { it >= 0 } }
        return exactMatch() ?: assignableMatch()
    }

    /**
     * Creates a comparison expression to check if the union holds a specific option.
     */
    fun isOption(union: LowLevelExpression.Field, index: Int): LowLevelExpression.Compare {
        return LowLevelExpression.Compare(
            optionIndex(union),
            LowLevelExpression.NumericalValue(index.toLong())
        )
    }

    /**
     * Switch on union discriminant, executing different code for each option.
     * Generates nested conditionals checking each option.
     * The callback receives the option field expression, the low-level option type, and the option index.
     */
    fun switch(
        union: LowLevelExpression.Field,
        caseBody: (optionField: LowLevelExpression.Field, optionType: LowLevelType) -> List<LowLevelInstruction>
    ): List<LowLevelInstruction> {
        val optionIdx = optionIndex(union)
        if (options.isEmpty()) TODO("Empty unions shjouldnt exist : $graphType")
        if (options.size == 1) TODO("Single field unions shjouldnt exist and be resolved to the type : $graphType")
        val conditional = options.indices.fold<Int, LowLevelInstruction.Conditional?>(null) { acc, idx ->
            val optionField = option(union, idx)
            val optionType = optionType(idx)
            val instructions = caseBody(optionField, optionType)

            if (instructions.isNotEmpty()) {
                LowLevelInstruction.Conditional(
                    isOption(union, idx),
                    instructions,
                    acc?.let { listOf(it) }
                )
            } else {
                acc
            }
        }

        return listOfNotNull(conditional)
    }

    /**
     * Write a value to a specific union option, setting the discriminant.
     */
    fun writeOption(
        target: LowLevelExpression.Field,
        optionIndex: Int,
        write: (openValue: LowLevelExpression.Field, type: LowLevelType) -> List<LowLevelInstruction>
    ): List<LowLevelInstruction> {
        return listOf(
            LowLevelInstruction.Write(
                optionIndex(target),
                LowLevelExpression.NumericalValue(optionIndex.toLong())
            )
        ) + write(option(target, optionIndex), optionType(optionIndex))
    }

    /**
     * Free union memory by switching on the discriminant and freeing the active option.
     */
    override fun free(field: LowLevelExpression.Field, ctx: LoweringContext): List<LowLevelInstruction> {
        return switch(field) { optionField, optionType ->
            optionType.free(optionField, ctx)
        }
    }

    override fun toString(): String {
        return options.joinToString(" | ") { it.graphType.toString() }
    }

    companion object {
        /**
         * Create a union type from a list of LowLevelType options.
         * Each option carries its own graphType.
         * Layout: { option: int, value: union { o0: T0, o1: T1, ... } }
         */
        fun of(union: Type.Union, options: List<LowLevelType>): JitsuUnion {
            require(options.isNotEmpty()) { "Union must have at least one option" }

            val valueMembers = options.mapIndexed { idx, opt -> "o$idx" to opt }.toMap()
            val valueUnion = LLUnion(valueMembers, union)
            val layout = LLStruct(
                mapOf(
                    "option" to uintTypeFor(options.size.toUInt()),
                    "value" to valueUnion
                ),
                union
            )
            return JitsuUnion(options, layout, union)
        }
        private fun uintTypeFor(max: UInt) = when {
            max <= UByte.MAX_VALUE -> uintTypeFor(BitSize.BIT_8)
            max <= UShort.MAX_VALUE -> uintTypeFor(BitSize.BIT_16)
            max <= UInt.MAX_VALUE -> uintTypeFor(BitSize.BIT_32)
            max <= ULong.MAX_VALUE -> uintTypeFor(BitSize.BIT_64)
            else -> throw IllegalArgumentException("UInt max value $max is too large (u64 max value is ${ULong.MAX_VALUE})")
        }

        private fun uintTypeFor(bits: BitSize) = TypeLowering.lower(object: Type.UInt {
            override val size: BitSize get() = bits
            override val rawType: Type get() = this

            override fun acceptsInstanceOf(type: Type): ReasonedBoolean {
                throw UnsupportedOperationException()
            }
            override val children: List<Element> get() = throw UnsupportedOperationException()
        })
    }
}
