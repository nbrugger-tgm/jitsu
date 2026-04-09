package eu.nitok.jitsu.compiler.bitcode

import eu.nitok.jitsu.compiler.graph.Type

sealed interface LowLevelInstruction {
    data class Free(val name: String) : LowLevelInstruction

    data class Alloc(val name: String, val layout: Type, val heap: Boolean): LowLevelInstruction

    data class Write(val name: String, val field: String?, val value: LowLevelExpression, val heap: Boolean): LowLevelInstruction
    data class Invoke(val name: String, val args: Map<String, LowLevelExpression>): LowLevelInstruction
    data class Return(val value: LowLevelExpression?): LowLevelInstruction
    data class Conditional(val condition: LowLevelExpression, val instructions: List<LowLevelInstruction>, val elseInstructions: List<LowLevelInstruction>?): LowLevelInstruction
}
