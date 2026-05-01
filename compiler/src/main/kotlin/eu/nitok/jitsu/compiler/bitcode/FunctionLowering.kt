package eu.nitok.jitsu.compiler.bitcode

import eu.nitok.jitsu.common.Range
import eu.nitok.jitsu.compiler.bitcode.LowLevelExpression.*
import eu.nitok.jitsu.compiler.bitcode.LowLevelInstruction.*
import eu.nitok.jitsu.compiler.graph.*
import eu.nitok.jitsu.compiler.graph.Function

/**
 * Result of lowering an expression.
 * @param expression The lowered expression
 * @param type The low-level type of the lowered expression (carries the graph type intrinsically)
 * @param instructions Instructions needed to evaluate the expression
 */
data class LoweredExpression(
    val expression: LowLevelExpression,
    val type: LowLevelType,
    val instructions: List<LowLevelInstruction>
)

/**
 * Lowers a high-level function to low-level instructions.
 *
 * This is a thin orchestrator that:
 * - Converts expressions to low-level IR
 * - Delegates type-specific operations to LowLevelType classes
 * - Uses LoweringContext for utilities (tmpVar, write)
 */
class FunctionLowering(
    val getUniqueName: (function: Function) -> String,
    val function: Function
) {
    val variableRegistry = VariableRegistry(function)
    val ctx = LoweringContext()

    fun lower(): List<LowLevelInstruction> {
        return when (function.body) {
            is Function.Body.Implementation -> lowerCodeBlock(
                function.body.block,
                function.returnType?.value?.let { TypeLowering.lower(it) }
            )

            is Function.Body.Native, Function.Body.Missing -> emptyList()
        }
    }

    private fun lowerCodeBlock(
        block: CodeBlock,
        expectedReturnType: LowLevelType?
    ): List<LowLevelInstruction> {
        val implicitReturn = if(block.instructions.lastOrNull() !is Instruction.Return && expectedReturnType == null)
            lowerReturn(Instruction.Return(null, Range(0,0,0,0)), null)
        else listOf()
        return block.instructions.flatMap { instruction ->
            when (instruction) {
                is Function -> TODO("Nested functions not yet supported!")
                is Instruction.FunctionCall -> lowerFunctionCallStatement(instruction)
                is Instruction.Return -> lowerReturn(instruction, expectedReturnType)
                is VariableDeclaration -> lowerVariableDeclaration(instruction)
            }
        } + implicitReturn
    }

    private fun lowerReturn(
        instruction: Instruction.Return,
        expectedReturnType: LowLevelType?
    ): List<LowLevelInstruction> {
        val freeInstructions = variableRegistry.variablesToFree.flatMap { entry ->
            entry.lowLevelType.free(entry.asVariable(), ctx)
        }

        val returnValue = instruction.value
        if (returnValue == null || expectedReturnType == null) {
            return freeInstructions + Return(null)
        }

        // Pass expected return type as hint for type-predictive lowering
        val lowered = lowerExpression(returnValue, expectedReturnType)

        // Convert only if needed (types still differ after hint-based lowering)
        val (convertedExpr, convertInstructions) = convert(
            lowered.expression, lowered.type, expectedReturnType
        )

        return lowered.instructions + convertInstructions + freeInstructions + Return(convertedExpr)
    }

    private fun lowerVariableDeclaration(variable: VariableDeclaration): List<LowLevelInstruction> {
        val entry = variableRegistry.getEntry(variable)
        val instructions = mutableListOf<LowLevelInstruction>()

        // Allocate on stack
        instructions += AllocStack(entry.name, entry.lowLevelType)

        // Let the type handle any heap allocation needed
        instructions += entry.lowLevelType.allocate(entry.asVariable())

        // Handle initial value
        val initialValue = variable.initialValue ?: return instructions

        // Pass target type as hint for type-predictive lowering
        val lowered = lowerExpression(initialValue, entry.lowLevelType)

        instructions += lowered.instructions
        instructions += writeValue(
            entry.lowLevelType, entry.asVariable(),
            lowered.expression, lowered.type
        )

        return instructions
    }

    private fun lowerFunctionCallStatement(call: Instruction.FunctionCall): List<LowLevelInstruction> {
        val target = call.target ?: TODO("function not found")
        val (invoke, instructions) = invokeFunction(call.parameters, target)
        return instructions + invoke
    }

    private fun invokeFunction(
        params: Map<String, Expression>,
        target: Function
    ): Pair<Invoke, List<LowLevelInstruction>> {
        val instructions = mutableListOf<LowLevelInstruction>()

        val args = params.mapValues { (name, expr) ->
            // Use parameter type as hint
            val paramType = target.parameters.find { it.name.value == name }?.declaredType?: TODO("Param not found!?")
            val hint = TypeLowering.lower(paramType)
            val lowered = lowerExpression(expr, hint)
            instructions += lowered.instructions
            var (convertExpr, conversionInstructions) = convert(lowered.expression,lowered.type, hint)
            instructions += conversionInstructions
            convertExpr
        }
        val invoke = Invoke(getUniqueName(target), args);
        return invoke to instructions
    }

    private fun lowerFunctionCallExpression(call: Instruction.FunctionCall): LoweredExpression {
        val target = call.target ?: TODO("function not found")
        val (invoke, instructions) = invokeFunction(call.parameters, target)
        val returnGraphType = target.returnType?.value
            ?: TODO("Trying to use a return less function as value??? $call")
        val returnType = TypeLowering.lower(returnGraphType)

        return LoweredExpression(ReturnValue(invoke), returnType, instructions)
    }

    /**
     * Lower an expression, optionally using a type hint for predictive lowering.
     * 
     * When a hint is provided, composite expressions (arrays, unions) will try to
     * construct themselves as the hinted type directly, avoiding unnecessary conversions.
     * 
     * @param expression The expression to lower
     * @param hint Optional target type hint - if provided, expression tries to match it directly
     * @return The lowered expression with its actual type and any instructions needed
     */
    private fun lowerExpression(
        expression: Expression,
        hint: LowLevelType? = null
    ): LoweredExpression {
        return when (expression) {
            is Constant.BooleanConstant -> LoweredExpression(
                NumericalValue(if (expression.value) 1L else 0L),
                LowLevelType.LLBool,
                emptyList()
            )

            is Constant.IntConstant -> LoweredExpression(
                NumericalValue(expression.value),
                TypeLowering.lower(expression.type),
                emptyList()
            )

            is Constant.UIntConstant -> LoweredExpression(
                NumericalValue(expression.value.toLong()),
                TypeLowering.lower(expression.type),
                emptyList()
            )

            is Constant.StringConstant -> TODO("String constants not yet supported")
            is Instruction.FunctionCall -> lowerFunctionCallExpression(expression)
            is Expression.Operation -> lowerFunctionCallExpression(expression.functionCall)
            is Expression.VariableReference -> lowerVariableReference(expression)
            is Expression.ArrayLiteral -> lowerArrayLiteral(expression, hint)
            is Expression.Undefined -> TODO("Cannot lower undefined expression")
        }
    }

    private fun lowerVariableReference(ref: Expression.VariableReference): LoweredExpression {
        val variableName = ref.reference.value
        val lowType = when (val target = ref.target) {
            is Function.Parameter -> {
                val gt = target.declaredType
                TypeLowering.lower(gt)
            }

            is VariableDeclaration -> {
                variableRegistry.getLowLevelType(target)
            }

            else -> TODO("Unknown variable target type")
        }

        val varExpr = LowLevelExpression.Variable(variableName)
        return LoweredExpression(varExpr, lowType, emptyList())
    }

    /**
     * Lower an array literal, using hint to determine element type if available.
     */
    private fun lowerArrayLiteral(
        literal: Expression.ArrayLiteral,
        hint: LowLevelType?
    ): LoweredExpression {
        // Use hint if it's a compatible array type, otherwise fall back to inferred type
        val arrayType = when {
            hint is JitsuArray && canUseArrayHint(literal, hint) -> hint
            else -> TypeLowering.lower(literal.type) as JitsuArray
        }

        val (tmpVar, tmpInstructions) = ctx.createTmpVar(arrayType)
        val instructions = tmpInstructions.toMutableList()

        // Allocate array data
        instructions += arrayType.alloc(tmpVar, literal.elements.size)

        // Write elements, using element type as hint
        literal.elements.forEachIndexed { index, element ->
            val elemLowered = lowerExpression(element, arrayType.elementType)
            instructions += elemLowered.instructions
            instructions += writeValue(
                arrayType.elementType,
                ArraySlot(arrayType.data(tmpVar), NumericalValue(index.toLong())),
                elemLowered.expression,
                elemLowered.type
            )
        }

        return LoweredExpression(tmpVar, arrayType, instructions)
    }

    /**
     * Check if we can use the hint array type for this literal.
     * The hint is usable if all elements can be assigned to the hint's element type.
     */
    private fun canUseArrayHint(literal: Expression.ArrayLiteral, hint: JitsuArray): Boolean {
        // For now, simple check: element count must match for fixed arrays
        if (hint.isFixedSize && hint.fixedSize != literal.elements.size) {
            return false
        }
        return true
    }

    // ==================== Type conversion ====================

    private fun convert(
        expression: LowLevelExpression,
        sourceLowType: LowLevelType,
        targetLowType: LowLevelType,
        targetVar: Field? = null
    ): Pair<LowLevelExpression, List<LowLevelInstruction>> {
        if (sourceLowType == targetLowType) {
            return expression to emptyList()
        }

        return when {
            sourceLowType is JitsuUnion -> convertFromUnion(expression, sourceLowType, targetLowType, targetVar)
            targetLowType is JitsuUnion -> convertToUnion(expression, sourceLowType, targetLowType, targetVar)
            sourceLowType is JitsuArray && targetLowType is JitsuArray -> convertArray(
                expression,
                sourceLowType,
                targetLowType,
                targetVar
            )

            else -> expression to emptyList()
        }
    }

    private fun convertToUnion(
        source: LowLevelExpression,
        sourceType: LowLevelType,
        targetType: JitsuUnion,
        targetVar: Field?
    ): Pair<LowLevelExpression, List<LowLevelInstruction>> {
        val (target, allocInstructions) = targetVar?.let { it to emptyList() } ?: ctx.createTmpVar(targetType)

        val sourceGraphType = sourceType.graphType
        val optionIndex = targetType.findOptionIndex(sourceGraphType)
            ?: error("Cannot find matching option for $sourceGraphType in $targetType")

        val writeInstructions = targetType.writeOption(target, optionIndex) { option, optionType ->
            writeValue(optionType, option, source, sourceType)
        }

        return target to allocInstructions + writeInstructions
    }

    private fun convertFromUnion(
        expression: LowLevelExpression,
        sourceUnion: JitsuUnion,
        targetType: LowLevelType,
        targetVar: Field?
    ): Pair<LowLevelExpression, List<LowLevelInstruction>> {
        val (sourceField, fieldInstructions) = ctx.asField(expression, sourceUnion)
        val (target, allocInstructions) = targetVar?.let { it to emptyList() } ?: ctx.createTmpVar(targetType)

        val switchInstructions = sourceUnion.switch(sourceField) { optionField, optionType ->
            writeValue(targetType, target, optionField, optionType)
        }

        return target to fieldInstructions + allocInstructions + switchInstructions
    }

    private fun convertArray(
        expression: LowLevelExpression,
        sourceArray: JitsuArray,
        targetArray: JitsuArray,
        targetVar: Field?
    ): Pair<LowLevelExpression, List<LowLevelInstruction>> {
        val (target, allocInstructions) = targetVar?.let { it to emptyList() } ?: ctx.createTmpVar(targetArray)
        val (sourceField, fieldInstructions) = ctx.asField(expression, sourceArray)

        val copyInstructions = copyArray(target, targetArray, sourceField, sourceArray)

        return target to allocInstructions + fieldInstructions + copyInstructions
    }

    private fun copyArray(
        target: Field,
        targetArray: JitsuArray,
        source: Field,
        sourceArray: JitsuArray
    ): List<LowLevelInstruction> {
        val instructions = mutableListOf<LowLevelInstruction>()

        // Allocate target if dynamic
        if (targetArray.isDynamic) {
            instructions += Write(targetArray.length(target), sourceArray.sizeExpression(source))
            instructions += Write(
                targetArray.data(target),
                AllocHeapArray(targetArray.elementType, sourceArray.sizeExpression(source))
            )
        }

        val srcElemType = sourceArray.elementType
        val tgtElemType = targetArray.elementType
        // Iterate and convert each element
        instructions += sourceArray.iterate(source, ctx) { srcElement, idx ->
            val targetElement = targetArray.accessIndex(target, idx)
            writeValue(tgtElemType, targetElement, srcElement, srcElemType)
        }

        return instructions
    }

    private fun writeValue(
        targetType: LowLevelType,
        target: Field,
        value: LowLevelExpression,
        sourceType: LowLevelType
    ): List<LowLevelInstruction> {
        val (convertedValue, convertInstructions) = convert(value, sourceType, targetType, target)
        return convertInstructions + ctx.write(targetType, target, convertedValue)
    }
}
