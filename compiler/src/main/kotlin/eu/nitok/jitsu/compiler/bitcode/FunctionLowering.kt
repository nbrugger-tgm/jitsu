package eu.nitok.jitsu.compiler.bitcode

import eu.nitok.jitsu.compiler.graph.CodeBlock
import eu.nitok.jitsu.compiler.graph.Constant
import eu.nitok.jitsu.compiler.graph.Expression
import eu.nitok.jitsu.compiler.graph.Function
import eu.nitok.jitsu.compiler.graph.Instruction
import eu.nitok.jitsu.compiler.graph.Type
import eu.nitok.jitsu.compiler.graph.VariableDeclaration


class FunctionLowering(
    val getUniqueName: (function: Function) -> String,
    val isReferenceType: (type: Type) -> Boolean,
) {

    fun lower(function: Function): List<LowLevelInstruction> {
        return when (function.body) {
            is Function.Body.Implementation -> lowerCodeBlock(function.body.block)
            is Function.Body.Native, Function.Body.Missing -> listOf()
        }
    }

    private fun lowerCodeBlock(block: CodeBlock): List<LowLevelInstruction> {
        return block.instructions.flatMap {
            when (it) {
                is Function -> TODO("Nested functions not yet supported!")
                is Instruction.FunctionCall -> it.lowerToInstruction()
                is Instruction.Return -> it.value?.lower().let {
                    if (it == null) listOf(LowLevelInstruction.Return(null))
                    else {
                        val (expression, instructs) = it
                        instructs + LowLevelInstruction.Return(expression)
                    }
                }

                is VariableDeclaration -> it.lower()
            }
        }
    }

    private fun VariableDeclaration.lower(): List<LowLevelInstruction> {
        val instructions = mutableListOf<LowLevelInstruction>()
        val isReference = isReferenceType(type)
        if (isReference) {
            instructions += LowLevelInstruction.Alloc(name.value, type)
        } else {
            instructions += LowLevelInstruction.StackAlloc(name.value, type)
        }
        if (this.initialValue == null) return instructions
        instructions += if (isReference) {
            LowLevelInstruction.WriteHeap(name.value, 0, this.initialValue.lower().first)
        } else {
            LowLevelInstruction.WriteStack(name.value, 0, this.initialValue.lower().first)
        }
        return instructions
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
                    LowLevelExpression.ReadHeap(reference.value) to emptyList()
                } else {
                    LowLevelExpression.ReadStack(reference.value) to emptyList()
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