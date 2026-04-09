package eu.nitok.jitsu.compiler.bitcode

import eu.nitok.jitsu.compiler.graph.*
import eu.nitok.jitsu.compiler.graph.Function


class FunctionLowering(
    val getUniqueName: (function: Function) -> String,
    val isReferenceType: (type: Type) -> Boolean,
) {
    var tmpVarIdx = 0;
    fun lower(function: Function): List<LowLevelInstruction> {
        return when (function.body) {
            is Function.Body.Implementation -> lowerCodeBlock(function.body.block, function.returnType?.value)
            is Function.Body.Native, Function.Body.Missing -> listOf()
        }
    }

    private fun lowerCodeBlock(block: CodeBlock, expectedReturnType: Type?): List<LowLevelInstruction> {
        return block.instructions.flatMap { instruct ->
            when (instruct) {
                is Function -> TODO("Nested functions not yet supported!")
                is Instruction.FunctionCall -> instruct.lowerToInstruction()
                is Instruction.Return -> instruct.value.let {
                    if (it == null || expectedReturnType == null) listOf(LowLevelInstruction.Return(null))
                    else {
                        val (expression, instructs) = it.lower()
                        val converted = convert(expression, it.type, expectedReturnType)
                        instructs + converted.second + LowLevelInstruction.Return(converted.first)
                    }
                }

                is VariableDeclaration -> instruct.lower()
            }
        }
    }

    private fun VariableDeclaration.lower(): List<LowLevelInstruction> {
        val instructions = mutableListOf<LowLevelInstruction>()
        val isReference = isReferenceType(type)
        instructions += LowLevelInstruction.Alloc(name.value, type, isReference)

        if (this.initialValue == null) return instructions
        val initialValue = this.initialValue.lower()
        instructions += initialValue.second
        instructions += write(type, name.value, initialValue.first, this.initialValue.type)
        return instructions
    }

    private fun write(
        targetType: Type,
        targetName: String,
        expression: LowLevelExpression,
        expressionType: Type
    ): List<LowLevelInstruction> {
        val convert = convert(expression, expressionType, targetType, targetVarName = targetName)
        return convert.second + listOf(
            LowLevelInstruction.Write(
                targetName,
                null,
                convert.first,
                heap = isReferenceType(targetType)
            )
        )
    }


    private fun writeUnion(
        targetType: Type.Union,
        targetName: String,
        expression: LowLevelExpression,
        expressionType: Type
    ): List<LowLevelInstruction> {
        return if (expressionType is Type.Union) {
            val sourceUnionVariable = asRead(expression, expressionType)
            val switch = unionSwitch(sourceUnionVariable.first.name, expressionType) { expr, type ->
                writeUnion(targetType, targetName, expr, type)
            }
            sourceUnionVariable.second + switch
        } else {
            val exactMatch = targetType.options.withIndex().find { it.value == expressionType }
            if (exactMatch != null) writeUnionOption(expression, exactMatch.value, targetName, exactMatch.index)
            else {
                val assignableMatch =
                    targetType.options.withIndex().find { it.value.acceptsInstanceOf(expressionType).value }
                        ?: TODO("$expressionType not assignable to any option in $targetType")
                val convertedValue = convert(expression, expressionType, assignableMatch.value)
                convertedValue.second + writeUnionOption(
                    convertedValue.first,
                    assignableMatch.value,
                    targetName,
                    assignableMatch.index
                )
            }
        }
    }

    private fun asRead(
        expression: LowLevelExpression,
        expressionType: Type
    ): Pair<LowLevelExpression.Read, List<LowLevelInstruction>> {
        if (expression is LowLevelExpression.Read) return expression to listOf()
        val capture = createTmpVar(expressionType)
        val write = write(expressionType, capture.name, expression, expressionType);
        return capture.expression to capture.instructions + write
    }

    data class Variable(
        val name: String,
        val expression: LowLevelExpression.Read,
        val instructions: List<LowLevelInstruction>
    )

    private fun createTmpVar(targetType: Type): Variable {
        val name = "tmp_${tmpVarIdx++}"
        val alloc = LowLevelInstruction.Alloc(name, targetType, isReferenceType(targetType))
        val read = LowLevelExpression.Read(name, isReferenceType(targetType))
        return Variable(name, read, listOf(alloc));
    }

    private fun convert(
        expression: LowLevelExpression,
        sourceType: Type,
        targetType: Type,
        targetVarName: String? = null
    ): Pair<LowLevelExpression, List<LowLevelInstruction>> {
        if (targetType is Type.Union) {
            val tmpVarName = if(targetVarName == null)createTmpVar(targetType)
            else Variable(targetVarName,LowLevelExpression.Read(targetVarName, isReferenceType(targetType)), listOf());
            val write = writeUnion(targetType, tmpVarName.name, expression, sourceType)
            val access = tmpVarName.expression
            return access to tmpVarName.instructions + write
        }
        if (sourceType is Type.Union) {
            val unionVar = asRead(expression, sourceType)
            val targetVar = if(targetVarName == null)createTmpVar(targetType)
            else Variable(targetVarName,LowLevelExpression.Read(targetVarName, isReferenceType(targetType)), listOf());
            val switch = unionSwitch(unionVar.first.name, sourceType) { expr, exprType ->
                write(targetType, targetVar.name, expr, exprType)
            }
            return targetVar.expression to unionVar.second + targetVar.instructions + switch
        }
        return expression to listOf()
    }

    private fun writeUnionOption(
        expression: LowLevelExpression,
        optionType: Type,
        targetName: String,
        option: Int
    ): List<LowLevelInstruction> = listOf(
        LowLevelInstruction.Write(
            targetName,
            "option",
            LowLevelExpression.NumericalValue(option.toLong()),
            heap = false
        ),
        LowLevelInstruction.Write("$targetName.value", "o${option}", expression, isReferenceType(optionType)),
    )

    private fun readUnionOptionIndex(unionVar: String): LowLevelExpression {
        return LowLevelExpression.Read("$unionVar.option", false)
    }

    private fun readUnionOption(unionVariable: String, unionOptionType: Type, idx: Int): LowLevelExpression {
        return LowLevelExpression.Read("$unionVariable.value.o$idx", isReferenceType(unionOptionType))
    }

    private fun unionSwitch(
        unionVariable: String,
        union: Type.Union,
        caseBody: (expression: LowLevelExpression, type: Type) -> List<LowLevelInstruction>
    ): List<LowLevelInstruction> {
        val unionOptionIdx = readUnionOptionIndex(unionVariable);
        return listOf(union.options.foldRightIndexed<Type, LowLevelInstruction.Conditional?>(null) { idx, option, conditional ->
            LowLevelInstruction.Conditional(
                LowLevelExpression.Compare(
                    unionOptionIdx,
                    LowLevelExpression.NumericalValue(idx.toLong())
                ), caseBody(
                    readUnionOption(unionVariable, option, idx),
                    option
                ), conditional?.let { listOf(it) })
        } ?: TODO("HOW??"))
    }

    private fun Expression.lower(): Pair<LowLevelExpression, List<LowLevelInstruction>> {
        return when (this) {
            is Constant.BooleanConstant -> LowLevelExpression.NumericalValue(1L) to emptyList()
            is Constant.IntConstant -> LowLevelExpression.NumericalValue(value) to emptyList()
            is Constant.UIntConstant -> LowLevelExpression.NumericalValue(value.toLong()) to emptyList()
            is Instruction.FunctionCall -> lowerToExpression()
            is Expression.Operation -> lower()
            is Expression.VariableReference -> {
                val type = when (val variable = target) {
                    is Function.Parameter -> variable.declaredType!!
                    is VariableDeclaration -> variable.type
                    else -> TODO()
                }
                if (isReferenceType(type)) {
                    LowLevelExpression.Read(reference.value, true) to emptyList()
                } else {
                    LowLevelExpression.Read(reference.value, false) to emptyList()
                }
            }

            is Constant.StringConstant -> TODO()
            is Expression.Undefined -> TODO()
        }
    }

    private fun Expression.Operation.lower(): Pair<LowLevelExpression, List<LowLevelInstruction>> {
        val target = target ?: TODO()
        val (leftExpr, leftInstr) = left.lower()
        val (rightExpr, rightInstr) = right.lower()
        val instructions = mutableListOf<LowLevelInstruction>()
        instructions += leftInstr
        instructions += rightInstr
        return LowLevelExpression.ReturnValue(
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
        return LowLevelExpression.ReturnValue(invoke) to instructions
    }
}