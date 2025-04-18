package eu.nitok.jitsu.compiler.analysis

import eu.nitok.jitsu.compiler.analysis.ExecutionState.VariableState.Ownership
import eu.nitok.jitsu.compiler.analysis.ParameterInfo.PassingType.*
import eu.nitok.jitsu.compiler.ast.CompilerMessages
import eu.nitok.jitsu.compiler.ast.Located
import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage.Hint
import eu.nitok.jitsu.compiler.graph.*
import eu.nitok.jitsu.compiler.graph.Function
import kotlinx.serialization.Serializable


@Serializable
data class ParameterInfo(
    val passingType: PassingType,
    val affectsOutput: Boolean
) {
}

@Serializable
data class ValueInfo(
    val deterministic: ReasonedBoolean,
    val type: Type,
    val affectedBy: List<ValueInfo> = listOf()
) {
}

@Serializable
data class FunctionInfo(
    val calls: List<FunctionInfo>,
    val returnInfo: ValueInfo?,
    val writeExternalState: Boolean = false,
    val parameters: Map<String, ParameterInfo>
) {
    val hasSideEffects: Boolean get() = writeExternalState || calls.any { it.hasSideEffects }
}

class ExecutionState(
    val localFunctions: MutableMap<String, MutableList<Function>> = mutableMapOf(),
    val localVariables: MutableMap<String, VariableState> = mutableMapOf(),
    private val parentScope: Scope
) {
    fun asScope(): Scope {
        return Scope(
            emptyList(),
            emptyMap(),
            localFunctions,
            localVariables.mapValues { it.value.declaration }
        ).also {
            it.parent = parentScope
        }
    }

    data class VariableState(
        val declaredType: Type?,
        var inferredType: Type,
        var ownershipState: Ownership,
        var readsExternal: Boolean,
        val possibleValues: List<ValueInfo>,
        val declaration: VariableDeclaration
    ) {
        val type get() = declaredType ?: inferredType;

        enum class Ownership {
            /**
             * This block owns the data and is resposible for cleaning it up
             */
            OWNS,

            /**
             * The block only reads the data and does now own it, and therefore needs not to clean it up
             */
            BORROWS,

            /**
             * The variable while still in scope is moved to another block and is not available anymore
             */
            MOVED
        }
    }
}

class CodeBlockAnalysis(
    private val block: CodeBlock,
    private val expectedReturnType: Type?,
    private val messages: CompilerMessages,
    parentScope: Scope
) {

    var returnInfo: ValueInfo? = null

    val calls: MutableList<FunctionInfo> = mutableListOf()
    val writeExternalState: Boolean = false;
    val parameters: MutableMap<String, ParameterInfo> = mutableMapOf()
    val executionState = ExecutionState(parentScope = parentScope)


    fun analyze() {
        for (instruction in block.instructions) {
            processInstruction(instruction)
        }
    }

    private fun processInstruction(instruction: Instruction) {
        if(instruction is ScopeAware) instruction.setEnclosingScope(executionState.asScope())
        if(instruction is Access<*>) instruction.finalize(messages)
        if(instruction is ScopeProvider) instruction.setScopes()
        when (instruction) {
            is Function -> {
                instruction.name?.let {
                    executionState.localFunctions.computeIfAbsent(it.value) { mutableListOf() }
                        .add(instruction)
                }
            }

            is Instruction.FunctionCall -> processFunctionCall(instruction)

            is Instruction.Return -> processReturn(instruction)

            is VariableDeclaration -> {
                val inferredType =
                    instruction.initialValue?.calculateType(executionState.localVariables.mapValues { it.value.type })
                        ?: Type.Undefined
                val initialValue = instruction.initialValue?.let { processExpression(it, BORROW) }
                if (instruction.declaredType != null && instruction.initialValue != null) {
                    val typeMatch = instruction.declaredType.acceptsInstanceOf(inferredType)
                    if (!typeMatch.value)
                        messages.error(typeMatch, instruction.initialValue.location)
                }
                val variableState = ExecutionState.VariableState(
                    declaredType = instruction.declaredType,
                    inferredType = inferredType,
                    ownershipState = Ownership.OWNS,
                    readsExternal = false,
                    possibleValues = listOfNotNull(initialValue),
                    declaration = instruction
                )
                instruction.implicitType = inferredType
                val varName = instruction.name
                if (executionState.localVariables.containsKey(varName.value)) {
                    messages.error(
                        "Variable ${varName.value} is already declared",
                        varName.location
                    )
                } else {
                    executionState.localVariables[varName.value] = variableState
                }
            }
        }
    }

    private fun processReturn(instruction: Instruction.Return) {
        val returnValueExpression = instruction.value;
        if (expectedReturnType != null && returnValueExpression == null) {
            messages.error(
                "Block requires a return value ($expectedReturnType), but no value was given", instruction.location,
                *listOfNotNull(instruction.function.name?.location?.let {
                    Hint(
                        "Return type defined here",
                        it
                    )
                }).toTypedArray()
            )

            return;
        } else if (returnValueExpression == null) {
            returnInfo = null
            return
        } else if (expectedReturnType == null) {
            messages.error("Block does not define a return type", returnValueExpression.location)
            return
        }
        returnInfo = processExpression(returnValueExpression, MOVE)
        val actualType = returnValueExpression.calculateType(executionState.localVariables.mapValues { it.value.type })
            ?: Type.Undefined
        val typeMatch = expectedReturnType.acceptsInstanceOf(actualType)
        if (!typeMatch.value)
            messages.error(typeMatch, returnValueExpression.location)
    }

    private fun processExpression(expression: Expression, ownershipType: ParameterInfo.PassingType): ValueInfo {
        if(expression is ScopeAware) expression.setEnclosingScope(executionState.asScope())
        if(expression is Access<*>) expression.finalize(messages)
        if(expression is ScopeProvider) expression.setScopes()
        val localVarTypes = executionState.localVariables.mapValues { it.value.type }
        return when (expression) {
            is Constant<*> -> ValueInfo(ReasonedBoolean.True("Constants are deterministic"), expression.type)
            is Instruction.FunctionCall -> processFunctionCall(expression) ?: run {
                messages.error(
                    "Function ${expression.reference.value} does not have a return type and cannot be used as value",
                    expression.reference.location
                )
                ValueInfo(ReasonedBoolean.True("Function does not have a return type"), Type.Undefined)
            }

            is Expression.Operation -> {
                val left = processExpression(expression.left, BORROW)
                val right = processExpression(expression.right, BORROW)
                ValueInfo(
                    deterministic = left.deterministic.and(right.deterministic),
                    type = expression.calculateType(localVarTypes) ?: Type.Undefined,
                    affectedBy = left.affectedBy + right.affectedBy
                )
            }

            is Expression.Undefined -> ValueInfo(
                ReasonedBoolean.False("Undefined expressions are not deterministic"),
                Type.Undefined
            )

            is Expression.VariableReference -> {
                val variableInfo = executionState.localVariables[expression.reference.value]
                if (variableInfo?.ownershipState == Ownership.MOVED) {
                    messages.error(
                        "Variable ${expression.reference.value} is moved and cannot be used anymore",
                        expression.reference.location
                    )
                }
                if (ownershipType == MOVE) moveVariable(expression.reference)
                val possibleValues = variableInfo?.possibleValues?.toTypedArray() ?: arrayOf()
                ValueInfo(
                    deterministic = possibleValues.map { it.deterministic }.reduceRightOrNull { a, b -> a.and(b) }
                        ?: ReasonedBoolean.False("Variable has no possible values"),
                    type = variableInfo?.type ?: Type.Undefined
                )
            }
        }
    }


    private fun moveVariable(varRef: Located<String>) {
        val variable = executionState.localVariables[varRef.value]
        if (variable != null) {
            variable.ownershipState = Ownership.MOVED
            return
        }
        messages.error(
            "Moving global is forbidden (wip)",
            varRef.location
        )
    }

    fun <T : Accessible<T>> Access<T>.resolve(resolver: () -> T?): T? {
        val target = resolver()
        target?.accessToSelf?.add(this)
        this.target = target
        return target
    }

    private fun processFunctionCall(functionCall: Instruction.FunctionCall): ValueInfo? {
        val localTypes = executionState.localVariables.mapValues { it.value.type }
        val actualParameterTypes =
            functionCall.callParameters.map { Located(it.calculateType(localTypes) ?: Type.Undefined, it.location) }
        functionCall.setEnclosingScope(executionState.asScope())
        functionCall.resolveAccessTarget(messages)
        val target = functionCall.resolve {
            functionCall.scope.resolveFunction(functionCall.reference, actualParameterTypes.toTypedArray(), messages)
        }
        if (target == null) {
            throw IllegalStateException("Function ${functionCall.reference.value} not resolved")
        }

        val formalParameters = target.info(messages).parameters
        val parameters = functionCall.callParameters.mapIndexed { i, exp ->
            val paramName = target.parameters.getOrNull(i)?.name?.value
            processExpression(exp, formalParameters[paramName]?.passingType ?: BORROW)
        }

        val returnInfo = target.info(messages).returnInfo ?: return null
        return ValueInfo(
            deterministic = parameters.map { it.deterministic }.reduceOrNull { a, b -> a.and(b) }
                ?.and(returnInfo.deterministic) ?: ReasonedBoolean.True("No parameters"),
            type = target.returnType!!
        );
    }


    fun toInfo(): FunctionInfo {
        return FunctionInfo(
            calls,
            returnInfo,
            writeExternalState,
            mapOf()
//            this@CodeBlockAnalysis.parameters.mapValues { ParameterInfo(it.value.passingType, true) }
        )
    }
}

fun Function.calculateFunctionInfo(messages: CompilerMessages): FunctionInfo {
    val analyzer = CodeBlockAnalysis(this.body, this.returnType, messages, scope)
    analyzer.analyze()
    return analyzer.toInfo()
}

