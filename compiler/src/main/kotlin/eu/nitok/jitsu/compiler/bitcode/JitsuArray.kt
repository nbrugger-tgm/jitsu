package eu.nitok.jitsu.compiler.bitcode

import eu.nitok.jitsu.compiler.bitcode.LowLevelType.Companion.I32
import eu.nitok.jitsu.compiler.bitcode.LowLevelType.Companion.I64
import eu.nitok.jitsu.compiler.bitcode.LowLevelType.*
import eu.nitok.jitsu.compiler.graph.Type

/**
 * Jitsu array type - a higher-level abstraction over LLStruct.
 *
 * This class provides typed access methods for array operations.
 */
class JitsuArray private constructor(
    val elementType: LowLevelType,
    val fixedSize: Int?,
    val layout: LLStruct,
) : Custom(layout) {
    val isFixedSize: Boolean get() = fixedSize != null
    val isDynamic: Boolean get() = fixedSize == null
    /**
     * Access the length field of a dynamic array.
     * @throws IllegalStateException if called on a fixed-size array
     */
    fun length(array: LowLevelExpression.Field): LowLevelExpression.Read {
        check(isDynamic) { "Fixed-size arrays don't have a length field, use fixedSize property" }
        return layout.accessField(array, "length")
    }

    /**
     * Access the data field (pointer for dynamic, or inline array for fixed).
     */
    fun data(array: LowLevelExpression.Field): LowLevelExpression.Read {
        return layout.accessField(array, "data")
    }

    /**
     * Access an element at a given index.
     * For dynamic arrays, this accesses through the data pointer.
     * For fixed arrays, this accesses the inline data directly.
     */
    fun accessIndex(array: LowLevelExpression.Field, index: LowLevelExpression): LowLevelExpression.ArraySlot {
        val dataField = data(array)
        return LowLevelExpression.ArraySlot(dataField, index)
    }

    /**
     * Get the size expression for iteration.
     * For fixed arrays, returns a constant. For dynamic, returns the length field.
     */
    fun sizeExpression(array: LowLevelExpression.Field): LowLevelExpression {
        return if (isFixedSize) {
            LowLevelExpression.NumericalValue(fixedSize!!.toLong())
        } else {
            length(array)
        }
    }

    /**
     * Iterate over array elements, generating loop instructions.
     */
    fun iterate(
        array: LowLevelExpression.Field,
        ctx: LoweringContext,
        body: (element: LowLevelExpression.Field, index: LowLevelExpression) -> List<LowLevelInstruction>
    ): List<LowLevelInstruction> {
        val sizeExpr = sizeExpression(array)

        val (counter, counterInstructs) = ctx.createTmpVar(I32)
        val initCounter = LowLevelInstruction.Write(counter, LowLevelExpression.NumericalValue(0))

        val elementAccess = accessIndex(array, counter)
        val loopBody = body(elementAccess, counter) + LowLevelInstruction.Increase(counter)

        return counterInstructs + initCounter + LowLevelInstruction.While(
            LowLevelExpression.CompareGreater(sizeExpr, counter),
            loopBody
        )
    }

    /**
     * Free array memory - elements first (if reference type), then data pointer (if dynamic).
     */
    override fun free(field: LowLevelExpression.Field, ctx: LoweringContext): List<LowLevelInstruction> {
        val elementCleanup = iterate(field, ctx) { element, _ ->
            elementType.free(element, ctx)
        }

        val dataCleanup = if (isDynamic) {
            listOf(LowLevelInstruction.Free(data(field)))
        } else {
            emptyList()
        }

        return elementCleanup + dataCleanup
    }

    /**
     * Allocate and initialize array data.
     */
    fun alloc(target: LowLevelExpression.Field, size: Int): List<LowLevelInstruction> {
        val instructions = mutableListOf<LowLevelInstruction>()
        if(isDynamic) instructions += setSize(target, size)
        if(isDynamic) instructions +=  LowLevelInstruction.Write(data(target), LowLevelExpression.AllocHeapArray(elementType, size))
        return instructions
    }

    private fun setSize(
        target: LowLevelExpression.Field,
        size: Int
    ): LowLevelInstruction = LowLevelInstruction.Write(length(target), LowLevelExpression.NumericalValue(size.toLong()))

    override fun toString(): String {
        return if (isFixedSize) "[$elementType; $fixedSize]" else "[$elementType]"
    }

    companion object {
        /**
         * Create a fixed-size array type.
         * Layout: { data: [T; size] }
         */
        fun fixed(elementType: LowLevelType, size: Int, graphType: Type): JitsuArray {
            val layout = LLStruct(
                mapOf("data" to LLFixedArray(elementType, size, graphType)),
                graphType
            )
            return JitsuArray(elementType, size, layout)
        }

        /**
         * Create a dynamic (heap-allocated) array type.
         * Layout: { length: i64, data: *T }
         */
        fun dynamic(elementType: LowLevelType, graphType: Type): JitsuArray {
            val layout = LLStruct(
                mapOf(
                    "length" to I64,
                    "data" to LLPointer(elementType, graphType)
                ),
                graphType
            )
            return JitsuArray(elementType, null, layout)
        }
    }
}
