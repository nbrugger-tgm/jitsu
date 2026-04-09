package eu.nitok.jitsu.compiler.bitcode

sealed interface LowLevelExpression {
    /**
     * A reference to a variable, in C this is equivalent to &name where name is the name of the variable.
     */
    data class Ref(val name: String) : LowLevelExpression

    /**
     * when heap false: A reference to a stack variable, in C this is equivalent to `name` where name is the name of the variable.
     *
     * when heap true: A "dereference" of a variable, in C this is equivalent to *name where name is the name of the variable.
     *    This is used to read if "name" is a reference to a heap allocated variable.
     */
    data class Read(val name: String, val heap: Boolean) : LowLevelExpression

    data class NumericalValue(val value: Long) : LowLevelExpression
    data class ReturnValue(val functionCall: LowLevelInstruction.Invoke) : LowLevelExpression
    data class Compare(val right: LowLevelExpression, val left: LowLevelExpression) : LowLevelExpression
}