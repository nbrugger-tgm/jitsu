package eu.nitok.jitsu.compiler.bitcode

import eu.nitok.jitsu.common.BitSize
import eu.nitok.jitsu.compiler.bitcode.LowLevelExpression.*
import eu.nitok.jitsu.compiler.bitcode.LowLevelInstruction.*
import eu.nitok.jitsu.compiler.graph.*
import eu.nitok.jitsu.compiler.graph.Function
import kotlin.Boolean
import kotlin.Int
import kotlin.Pair
import kotlin.String
import kotlin.TODO
import kotlin.let
import kotlin.to


class FunctionLowering(
    val getUniqueName: (function: Function) -> String,
    val isReferenceType: (type: Type) -> Boolean,
    val function: Function
) {
    val variableRegistry = VariableRegistry(function)
    var tmpVarIdx = 0;
    fun lower(): List<LowLevelInstruction> {
        return when (function.body) {
            is Function.Body.Implementation -> lowerCodeBlock(
                function.body.block,
                function.returnType?.value,
                variableRegistry
            )

            is Function.Body.Native, Function.Body.Missing -> listOf()
        }
    }

    private fun lowerCodeBlock(
        block: CodeBlock,
        expectedReturnType: Type?,
        variableRegistry: VariableRegistry
    ): List<LowLevelInstruction> {
        return block.instructions.flatMap { instruct ->
            when (instruct) {
                is Function -> TODO("Nested functions not yet supported!")
                is Instruction.FunctionCall -> instruct.lowerToInstruction()
                is Instruction.Return -> lowerReturn(instruct, expectedReturnType, variableRegistry)
                is VariableDeclaration -> instruct.lower()
            }
        }
    }

    private fun lowerReturn(
        instruct: Instruction.Return,
        expectedReturnType: Type?,
        variableSummary: VariableRegistry
    ): List<LowLevelInstruction> = instruct.value.let {
        var frees = variableSummary.variablesToFree.flatMap { variable ->
            free(variable)
        }
        if (it == null || expectedReturnType == null) frees + listOf(Return(null))
        else {
            val (expression, instructs) = it.lower()
            val converted = convert(expression, it.type, expectedReturnType)
            instructs + converted.second + frees + Return(converted.first)
        }
    }

    private fun free(variableInfo: VariableRegistry.Entry): List<LowLevelInstruction> {
        val variable = LowLevelExpression.Variable(variableInfo.name)
        return if (variableInfo.type is Type.Union) {
            unionSwitch(variable, variableInfo.type) { ex, type ->
                if (isReferenceType(type)) listOf(Free(ex))
                else listOf()
            }
        } else if (variableInfo.type is Type.Array) {
            val arrayData = Read(variable, "data")
            val elementCleanup = if (isReferenceType(variableInfo.type.elementType)) {
                val arraySize = arraySize(variable)
                val counter = createTmpVar(Type.Int(BitSize.BIT_32))
                counter.second + While(
                    CompareGreater(arraySize, counter.first), listOf(
                        Free(ArraySlot(arrayData, counter.first)),
                        Increase(counter.first)
                    )
                )
            } else listOf()
            val arrayCleanup = if (variableInfo.type.size == null) {
                listOf(Free(arrayData))
            } else listOf()
            elementCleanup + arrayCleanup
        } else {
            listOf(Free(variable))
        }
    }

    private fun arraySize(name: Field): LowLevelExpression {
        return Read(name, "size")
    }

    private fun VariableDeclaration.lower(): List<LowLevelInstruction> {
        val instructions = mutableListOf<LowLevelInstruction>()
        val name = variableRegistry.getEntry(this).name
        val variable = AllocStack(name, type)
        instructions += variable
        val isRef = isReferenceType(type)
        val varExpr = LowLevelExpression.Variable(name)
        if (isRef) {
            instructions += Write(varExpr, AllocHeap(type))
        }
        if (initialValue == null) return instructions
        val initialValue = this.initialValue.lower()
        instructions += initialValue.second
        instructions += write(type, varExpr, initialValue.first, this.initialValue.type)
        return instructions
    }

    private fun write(
        targetType: Type,
        targetName: Field,
        expression: LowLevelExpression,
        expressionType: Type,
    ): List<LowLevelInstruction> {
        val convert = convert(expression, expressionType, targetType, targetVarName = targetName)
        return convert.second + listOf(
            Write(if (isReferenceType(targetType)) Deref(targetName) else targetName, convert.first)
        )
    }


    private fun writeUnion(
        targetType: Type.Union,
        targetUnion: Field,
        expression: LowLevelExpression,
        expressionType: Type
    ): List<LowLevelInstruction> {
        return if (expressionType is Type.Union) {
            val sourceUnionVariable = asField(expression, expressionType)
            val switch = unionSwitch(sourceUnionVariable.first, expressionType) { expr, type ->
                writeUnion(targetType, targetUnion, expr, type)
            }
            sourceUnionVariable.second + switch
        } else {
            val exactMatch = targetType.options.withIndex().find { it.value == expressionType }
            if (exactMatch != null) writeUnionOption(expression, exactMatch.value, targetUnion, exactMatch.index)
            else {
                val assignableMatch =
                    targetType.options.withIndex().find { it.value.acceptsInstanceOf(expressionType).value }
                        ?: TODO("$expressionType not assignable to any option in $targetType")
                val convertedValue = convert(expression, expressionType, assignableMatch.value)
                convertedValue.second + writeUnionOption(
                    convertedValue.first,
                    assignableMatch.value,
                    targetUnion,
                    assignableMatch.index
                )
            }
        }
    }

    private fun asField(
        expression: LowLevelExpression,
        expressionType: Type
    ): Pair<Field, List<LowLevelInstruction>> {
        if (expression is Field) return expression to listOf()
        val capture = createTmpVar(expressionType)
        val write = write(expressionType, capture.first, expression, expressionType);
        return capture.first to capture.second + write
    }


    private fun createTmpVar(targetType: Type): Pair<Field, List<LowLevelInstruction>> {
        val name = "tmp_${tmpVarIdx++}"
        val stackVariable = AllocStack(name, targetType)
        val init = if(isReferenceType(targetType)) {
            Write(LowLevelExpression.Variable(stackVariable.name), AllocHeap(targetType))
        } else null
        return LowLevelExpression.Variable(stackVariable.name) to listOfNotNull(stackVariable, init)
    }

    private fun convert(
        expression: LowLevelExpression,
        sourceType: Type,
        targetType: Type,
        targetVarName: Field? = null
    ): Pair<LowLevelExpression, List<LowLevelInstruction>> {
        if (sourceType == targetType) {
            return expression to listOf()
        }
        if (targetType is Type.Union) {
            val tmpVarName = if (targetVarName == null) createTmpVar(targetType)
            else targetVarName to listOf();
            val write = writeUnion(targetType, tmpVarName.first, expression, sourceType)
            val access = tmpVarName.first
            return access to tmpVarName.second + write
        }
        if (sourceType is Type.Union) {
            val unionVar = asField(expression, sourceType)
            val targetVar = if (targetVarName == null) createTmpVar(targetType)
            else targetVarName to listOf();
            val switch = unionSwitch(unionVar.first, sourceType) { expr, exprType ->
                write(targetType, targetVar.first, expr, exprType)
            }
            return targetVar.first to unionVar.second + targetVar.second + switch
        }
        return expression to listOf()
    }

    private fun writeUnionOption(
        expression: LowLevelExpression,
        optionType: Type,
        targetName: Field,
        option: Int
    ): List<LowLevelInstruction> = listOf(
        Write(
            Read(targetName, "option"),
            NumericalValue(option.toLong())
        ),
        Write(unionOption(targetName, optionType, option), expression),
    )

    private fun unionOptionIndex(unionVar: Field): Field {
        return Read(unionVar, "option")
    }

    private fun unionOption(unionVariable: Field, unionOptionType: Type, idx: Int): Field {
        val union = Read(unionVariable, "value")
        val optionValue = Read(union, "o$idx")
        return if (isReferenceType(unionOptionType)) Deref(optionValue) else optionValue
    }

    private fun unionSwitch(
        unionVariable: Field,
        union: Type.Union,
        caseBody: (expression: Field, type: Type) -> List<LowLevelInstruction>
    ): List<LowLevelInstruction> {
        val unionOptionIdx = unionOptionIndex(unionVariable);
        return listOf(union.options.foldRightIndexed<Type, Conditional?>(null) { idx, option, conditional ->
            val instructions = caseBody(unionOption(unionVariable, option, idx), option)
            if (instructions.isNotEmpty()) Conditional(
                Compare(unionOptionIdx, NumericalValue(idx.toLong())),
                instructions, conditional?.let { listOf(it) }
            ) else conditional
        } ?: TODO("HOW??"))
    }

    private fun Expression.lower(): Pair<LowLevelExpression, List<LowLevelInstruction>> {
        return when (this) {
            is Constant.BooleanConstant -> NumericalValue(1L) to emptyList()
            is Constant.IntConstant -> NumericalValue(value) to emptyList()
            is Constant.UIntConstant -> NumericalValue(value.toLong()) to emptyList()
            is Instruction.FunctionCall -> lowerToExpression()
            is Expression.Operation -> lower()
            is Expression.VariableReference -> {
                val type = when (val variable = target) {
                    is Function.Parameter -> variable.declaredType!!
                    is VariableDeclaration -> variable.type
                    else -> TODO()
                }
                if (isReferenceType(type)) {
                    Deref(LowLevelExpression.Variable(reference.value)) to emptyList()
                } else {
                    LowLevelExpression.Variable(reference.value) to emptyList()
                }
            }

            is Constant.StringConstant -> TODO()
            is Expression.Undefined -> TODO()
            is Expression.ArrayLiteral -> lower()
        }
    }

    private fun Expression.ArrayLiteral.lower(): Pair<LowLevelExpression, List<LowLevelInstruction>> {
        val tmpVar = createTmpVar(type)
        val arrayAlloc = if (type.size == null) listOf(Write(Read(tmpVar.first, "data"), AllocHeapArray(type.elementType, elements.size)))
        else listOf()
        val writes = elements.flatMapIndexed { index, expression ->
            val loweredExpression = expression.lower()
            loweredExpression.second + WriteAtIndex(
                tmpVar.first,
                LowLevelExpression.NumericalValue(index.toLong()),
                loweredExpression.first
            )
        }
        return tmpVar.first to tmpVar.second + arrayAlloc + writes
    }

    private fun Expression.Operation.lower(): Pair<LowLevelExpression, List<LowLevelInstruction>> {
        val target = target ?: TODO()
        val (leftExpr, leftInstr) = left.lower()
        val (rightExpr, rightInstr) = right.lower()
        val instructions = mutableListOf<LowLevelInstruction>()
        instructions += leftInstr
        instructions += rightInstr
        return ReturnValue(
            LowLevelInstruction.Invoke(
                getUniqueName(target), mapOf(
                    target.parameters[0].name.value to leftExpr,
                    target.parameters[1].name.value to rightExpr
                )
            )
        ) to instructions
    }

    private fun Instruction.FunctionCall.lowerToInstruction(): List<LowLevelInstruction> {
        val function = target ?: TODO("function not found how to deal with that?")
        val instructions = mutableListOf<LowLevelInstruction>()
        val argParams = parameters.mapValues { (_, it) ->
            val (expr, instr) = it.lower()
            instructions += instr
            expr
        }
        val invoke = LowLevelInstruction.Invoke(
            getUniqueName(function),
            argParams
        )
        instructions += invoke
        return instructions
    }

    private fun Instruction.FunctionCall.lowerToExpression(): Pair<LowLevelExpression, List<LowLevelInstruction>> {
        val function = target ?: TODO("function not found how to deal with that?")
        val instructions = mutableListOf<LowLevelInstruction>()
        val argParams = parameters.mapValues { (_, it) ->
            val (expr, instr) = it.lower()
            instructions += instr
            expr
        }
        val invoke = LowLevelInstruction.Invoke(
            getUniqueName(function),
            argParams
        )
        return ReturnValue(invoke) to instructions
    }
}