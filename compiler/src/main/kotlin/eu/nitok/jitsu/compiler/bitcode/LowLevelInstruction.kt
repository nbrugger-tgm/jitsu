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
    data class Free(val target: Field) : LowLevelInstruction {
        override fun toString(): String {
            return "free($target)"
        }
    }

    /**
     * Allocate space on the stack for a variable.
     */
    data class AllocStack(val name: String, val layout: LowLevelType) : LowLevelInstruction {
        override fun toString(): String {
            return "$name: $layout"
        }
    }

    /**
     * Write a value to a location.
     */
    data class Write(val target: Field, val value: LowLevelExpression) : LowLevelInstruction {
        override fun toString(): String {
            return "$target = $value"
        }
    }

    /**
     * Call a function.
     */
    data class Invoke(val functionName: String, val args: Map<String, LowLevelExpression>) : LowLevelInstruction {
        override fun toString(): String {
            return "$functionName($args)"
        }
    }

    /**
     * Return from the current function.
     */
    data class Return(val value: LowLevelExpression?) : LowLevelInstruction {
        override fun toString(): String {
            return if (value != null) "return $value"
            else "return"
        }
    }

    /**
     * Conditional execution (if-else).
     */
    data class Conditional(
        val condition: LowLevelExpression,
        val thenInstructions: List<LowLevelInstruction>,
        val elseInstructions: List<LowLevelInstruction>?
    ) : LowLevelInstruction {
        override fun toString(): String {
            return "if($condition) {\n${
                thenInstructions.joinToString("\n") { it.toString() }.prependIndent("  ")
            }\n}${
                elseInstructions?.joinToString("\n", prefix = "else {\n", postfix = "\n}") {
                    it.toString().prependIndent("  ")
                }
            }}"
        }
    }

    /**
     * While loop.
     */
    data class While(
        val condition: LowLevelExpression,
        val body: List<LowLevelInstruction>
    ) : LowLevelInstruction {
        override fun toString(): String {
            return "while($condition) {\n${body.joinToString("\n") { it.toString() }.prependIndent("  ")}\n}"
        }
    }

    /**
     * Increment a numeric variable by 1.
     */
    data class Increase(val variable: Field) : LowLevelInstruction {
        override fun toString(): String {
            return "$variable++"
        }
    }
}
