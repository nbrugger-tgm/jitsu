package eu.nitok.jitsu.compiler.bitcode

import eu.nitok.jitsu.compiler.bitcode.LowLevelExpression.Field

/**
 * Low-level instructions for bytecode generation.
 * These instructions are independent of the high-level graph types.
 */
sealed interface LowLevelInstruction {
    /**
     * Free heap-allocated memory.
     */
    data class Free(val target: Field) : LowLevelInstruction

    /**
     * Allocate space on the stack for a variable.
     */
    data class AllocStack(val name: String, val layout: LowLevelType) : LowLevelInstruction

    /**
     * Write a value to a location.
     */
    data class Write(val target: Field, val value: LowLevelExpression) : LowLevelInstruction

    /**
     * Call a function.
     */
    data class Invoke(val functionName: String, val args: Map<String, LowLevelExpression>) : LowLevelInstruction

    /**
     * Return from the current function.
     */
    data class Return(val value: LowLevelExpression?) : LowLevelInstruction

    /**
     * Conditional execution (if-else).
     */
    data class Conditional(
        val condition: LowLevelExpression,
        val thenInstructions: List<LowLevelInstruction>,
        val elseInstructions: List<LowLevelInstruction>?
    ) : LowLevelInstruction

    /**
     * While loop.
     */
    data class While(
        val condition: LowLevelExpression,
        val body: List<LowLevelInstruction>
    ) : LowLevelInstruction

    /**
     * Increment a numeric variable by 1.
     */
    data class Increase(val variable: Field) : LowLevelInstruction
}
