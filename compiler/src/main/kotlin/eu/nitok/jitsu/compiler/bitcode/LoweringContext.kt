package eu.nitok.jitsu.compiler.bitcode

/**
 * Context for lowering operations.
 * Provides utilities like temporary variable generation.
 * Type-specific operations live in the types themselves.
 */
class LoweringContext {
    private var tmpVarIdx = 0

    /**
     * Generate a unique temporary variable name.
     */
    fun nextTmpName(): String = "tmp_${tmpVarIdx++}"

    /**
     * Create a temporary variable on the stack.
     * Returns the variable expression and the allocation instructions.
     */
    fun createTmpVar(type: LowLevelType): Pair<LowLevelExpression.Variable, List<LowLevelInstruction>> {
        val name = nextTmpName()
        val alloc = LowLevelInstruction.AllocStack(name, type)
        val varExpr = LowLevelExpression.Variable(name)

        return varExpr to listOf(alloc) + type.allocate(varExpr)
    }

    /**
     * Capture an expression into a temporary variable if it's not already a field.
     * Returns the field and any instructions needed to create the temporary.
     */
    fun asField(
        expression: LowLevelExpression,
        type: LowLevelType
    ): Pair<LowLevelExpression.Field, List<LowLevelInstruction>> {
        if (expression is LowLevelExpression.Field) return expression to emptyList()

        val (tmpVar, allocInstructions) = createTmpVar(type)
        val writeInstructions = write(type, tmpVar, expression)
        return tmpVar to allocInstructions + writeInstructions
    }

    /**
     * Write a value to a location, using the type's write access logic.
     */
    fun write(
        targetType: LowLevelType,
        target: LowLevelExpression.Field,
        value: LowLevelExpression
    ): List<LowLevelInstruction> {
        return listOf(LowLevelInstruction.Write(targetType.writeAccess(target), value))
    }
}
