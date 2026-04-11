package eu.nitok.jitsu.compiler.bitcode

/**
 * Low-level expressions for bytecode generation.
 * These expressions are independent of the high-level graph types.
 */
sealed interface LowLevelExpression {
    /**
     * Expressions that represent addressable locations (l-values).
     */
    sealed interface Field : LowLevelExpression

    /**
     * A reference to a variable, in C this is equivalent to &name where name is the name of the variable.
     */
    data class Ref(val name: Field) : Field

    /**
     * Reads the content the pointer {name} points to. In C: *name
     */
    data class Deref(val name: Field) : Field

    /**
     * Access a field of a struct. In C: struct.name or struct->name
     */
    data class Read(val struct: Field, val name: String) : Field

    /**
     * Access an array element at an index. In C: array[index]
     */
    data class ArraySlot(val array: Field, val index: LowLevelExpression) : Field

    /**
     * A named variable reference.
     */
    data class Variable(val name: String) : Field

    /**
     * Allocate memory on the heap for the given type layout.
     */
    data class AllocHeap(val layout: LowLevelType) : LowLevelExpression

    /**
     * Allocate an array on the heap.
     * @param elementType The type of each element
     * @param size The number of elements (can be static or dynamic)
     */
    data class AllocHeapArray(val elementType: LowLevelType, val size: LowLevelExpression) : LowLevelExpression {
        constructor(elementType: LowLevelType, staticSize: Int) : this(
            elementType,
            NumericalValue(staticSize.toLong())
        )
    }

    /**
     * A numeric constant value.
     */
    data class NumericalValue(val value: Long) : LowLevelExpression

    /**
     * The return value of a function call.
     */
    data class ReturnValue(val functionCall: LowLevelInstruction.Invoke) : LowLevelExpression

    /**
     * Equality comparison. Returns true if left == right.
     */
    data class Compare(val left: LowLevelExpression, val right: LowLevelExpression) : LowLevelExpression

    /**
     * Greater-than comparison. Returns true if left > right.
     */
    data class CompareGreater(val left: LowLevelExpression, val right: LowLevelExpression) : LowLevelExpression
}
