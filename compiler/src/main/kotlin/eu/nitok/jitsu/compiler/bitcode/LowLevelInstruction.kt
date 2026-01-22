package eu.nitok.jitsu.compiler.bitcode

sealed interface LowLevelInstruction {
    data class Free(val name: String) : LowLevelInstruction

    data class Alloc(val name: String, val layout: MemoryFragment): LowLevelInstruction
    data class StackAlloc(val name: String, val layout: MemoryFragment): LowLevelInstruction

    data class WriteStack(val name: String, val offset: Long, val value: LowLevelExpression): LowLevelInstruction
    data class WriteHeap(val name: String, val offset: Long, val value: LowLevelExpression): LowLevelInstruction

    data class Invoke(val name: String, val args: List<Param>): LowLevelInstruction {
        data class Param(val name:String, val value: LowLevelExpression)
    }
    data class Return(val value: LowLevelExpression?): LowLevelInstruction
}
