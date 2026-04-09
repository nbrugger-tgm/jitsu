package eu.nitok.jitsu.compiler.bitcode

import eu.nitok.jitsu.compiler.graph.Type

sealed interface LowLevelExpression {
    sealed interface Field : LowLevelExpression {}
    /**
     * A reference to a variable, in C this is equivalent to &name where name is the name of the variable.
     */
    data class Ref(val name: Field) : Field

    /**
     * Reads the content the pointer {name} points to in c thats *name
     */
    data class Deref(val name: Field) : Field
    data class Read(val struct: Field, val name: String) : Field
    data class ArraySlot(val array: Field, val index: LowLevelExpression) : Field
    data class Variable(val name: String) : Field

    data class AllocHeap(val type: Type): LowLevelExpression
    data class AllocHeapArray(val type: Type, val arraySize: Int): LowLevelExpression
    data class NumericalValue(val value: Long) : LowLevelExpression
    data class ReturnValue(val functionCall: LowLevelInstruction.Invoke) : LowLevelExpression
    data class Compare(val right: LowLevelExpression, val left: LowLevelExpression) : LowLevelExpression
    data class CompareGreater(val right: LowLevelExpression, val left: LowLevelExpression) : LowLevelExpression
}