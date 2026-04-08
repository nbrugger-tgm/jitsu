package eu.nitok.jitsu.compiler.bitcode

import eu.nitok.jitsu.compiler.graph.Type

sealed interface LowLevelInstruction {
    data class Free(val name: String) : LowLevelInstruction

    data class Alloc(val name: String, val layout: Type): LowLevelInstruction
    data class StackAlloc(val name: String, val layout: Type): LowLevelInstruction

    data class WriteStack(val name: String, val offset: Long, val value: LowLevelExpression): LowLevelInstruction
    data class WriteHeap(val name: String, val offset: Long, val value: LowLevelExpression): LowLevelInstruction
    data class Invoke(val name: String, val args: Map<String, LowLevelExpression>): LowLevelInstruction
    data class Return(val value: LowLevelExpression?): LowLevelInstruction
}
