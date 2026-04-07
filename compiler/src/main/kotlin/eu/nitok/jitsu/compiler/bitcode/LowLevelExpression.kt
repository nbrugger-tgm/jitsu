package eu.nitok.jitsu.compiler.bitcode

sealed interface LowLevelExpression {
    /**
     * A reference to a variable, in C this is equivalent to &name where name is the name of the variable.
     */
    data class Ref(val name: String): LowLevelExpression

    /**
     * A reference to a stack variable, in C this is equivalent to `name` where name is the name of the variable.
     */
    data class ReadStack(val name: String): LowLevelExpression

    /**
     * A "dereference" of a variable, in C this is equivalent to *name where name is the name of the variable.
     * This is used to read if "name" is a reference to a heap allocated variable.
     */
    data class ReadHeap(val name: String): LowLevelExpression
    data class NumericalValue(val value: Long): LowLevelExpression
    data class Alloc(val layout: MemoryLayout): LowLevelExpression
    data class ReturnValue(val functionCall: LowLevelInstruction.Invoke): LowLevelExpression
}