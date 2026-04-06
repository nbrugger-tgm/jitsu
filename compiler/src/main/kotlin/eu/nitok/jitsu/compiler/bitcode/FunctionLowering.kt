package eu.nitok.jitsu.compiler.bitcode

import eu.nitok.jitsu.compiler.graph.CodeBlock
import eu.nitok.jitsu.compiler.graph.Constant
import eu.nitok.jitsu.compiler.graph.Expression
import eu.nitok.jitsu.compiler.graph.Function
import eu.nitok.jitsu.compiler.graph.Instruction
import eu.nitok.jitsu.compiler.graph.Type
import eu.nitok.jitsu.compiler.graph.VariableDeclaration


class FunctionLowering(val functionNameRegistry: FunctionNameRegistry, val function: Function) {

    var tmpVarIdx = 0

    fun nextTmpVarName(): String {
        val name = "${functionNameRegistry.getUniqueName(function)}_tmp$tmpVarIdx"
        tmpVarIdx++
        return name
    }

    fun lower(): List<LowLevelInstruction> {
        return when (function.body) {
            is Function.Body.Implementation -> lower(function.body.block)
            is Function.Body.Native -> lower(
                function.returnType,
                function.parameters.map { it.name.value },
                function.body
            )
            Function.Body.Missing -> listOf()
        }
    }

    private fun lower(returnType: Type?, params: List<String>, block: Function.Body.Native): List<LowLevelInstruction> {
        val invoke = LowLevelInstruction.Invoke(
            block.nativeTarget,
            params.map { LowLevelInstruction.Invoke.Param(it, LowLevelExpression.ReadStack(it)) }
        );
        return if(returnType == null) {
            listOf(invoke)
        } else {
            listOf(LowLevelInstruction.Return(LowLevelExpression.ReturnValue(invoke)))
        }
    }

    private fun lower(block: CodeBlock): List<LowLevelInstruction> {
        return block.instructions.flatMap {
            when (it) {
                is Function -> TODO()
                is Instruction.FunctionCall -> it.lower().let { (expression, instructs) ->
                    instructs + expression.functionCall
                }

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
        var layout = type.layout
        val instructions = mutableListOf<LowLevelInstruction>()
        if (layout.size > STACK_ALLOC_LIMIT) {
            val referencedLayout = layout
            layout = MemoryFragment.Reference(layout)
            instructions += LowLevelInstruction.Alloc(name.value, referencedLayout)
        } else {
            instructions += LowLevelInstruction.StackAlloc(name.value, layout)
        }
        if (this.initialValue == null) return instructions
        instructions += if (layout is MemoryFragment.Reference) {
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
            is Instruction.FunctionCall -> lower()
            is Expression.Operation -> lower()
            is Expression.VariableReference -> {
                val layout = when (val variable = target) {
                    is Function.Parameter -> variable.declaredType!!.layout
                    is VariableDeclaration -> variable.type.layout
                    else -> TODO()
                }
                if (layout is MemoryFragment.Reference) {
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
                functionNameRegistry.getUniqueName(target), listOf(
                    LowLevelInstruction.Invoke.Param(target.parameters[0].name.value, leftExpr),
                    LowLevelInstruction.Invoke.Param(target.parameters[1].name.value, rightExpr)
                )
            )
        ) to instructions
    }

    private fun Instruction.FunctionCall.lower(): Pair<LowLevelExpression.ReturnValue, List<LowLevelInstruction>> {
        val function = target ?: TODO()
        val instructions = mutableListOf<LowLevelInstruction>()
        val argParams = parameters.map { (name, it) ->
            val (expr, instr) = it.lower()
            instructions += instr
            LowLevelInstruction.Invoke.Param(name, expr)
        }
        return Pair(
            LowLevelExpression.ReturnValue(
                LowLevelInstruction.Invoke(
                    functionNameRegistry.getUniqueName(function),
                    argParams
                )
            ),
            instructions
        )
    }
}