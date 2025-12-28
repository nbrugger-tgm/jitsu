package eu.nitok.jitsu.compiler.analysis

import eu.nitok.jitsu.compiler.analysis.ExecutionState.VariableState.Ownership
import eu.nitok.jitsu.compiler.analysis.PassingType.*
import eu.nitok.jitsu.compiler.ast.CompilerMessages
import eu.nitok.jitsu.compiler.ast.Located
import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage.Hint
import eu.nitok.jitsu.compiler.graph.*
import eu.nitok.jitsu.compiler.graph.Function
import kotlinx.serialization.Serializable

enum class PassingType {
    BORROW, MOVE
}

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
    val writeExternalState: ReasonedBoolean = ReasonedBoolean.False("On their own functions do not write external state"),
    val parameters: Map<String, ParameterInfo>
) {
    val hasSideEffects: ReasonedBoolean
        get() = writeExternalState.or(
            calls.map { it.hasSideEffects }.any() ?: ReasonedBoolean.False("Doesn't call any functions")
        )
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
        var inferredType: Type?,
        var ownershipState: Ownership,
        var readsExternal: ReasonedBoolean,
        val possibleValues: List<ValueInfo>,
        val declaration: Variable
    ) {
        val type get() = declaredType ?: inferredType ?: Type.Undefined;

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
    val writeExternalState: ReasonedBoolean = ReasonedBoolean.False("no external variable is accessed");
    val parameters: MutableMap<String, ParameterInfo> = mutableMapOf()
    val executionState = ExecutionState(parentScope = parentScope)


    fun analyze() {
        for (instruction in block.instructions) {
            processInstruction(instruction)
        }
    }

    private fun processInstruction(instruction: Instruction) {
        if (instruction is ScopeAware) instruction.setEnclosingScope(executionState.asScope())
        if (instruction is Access<*>) instruction.finalize(messages)
        if (instruction is ScopeProvider) instruction.setScopes()
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
                //if we only borrow the value here what if we later move the value somewhere?
                //does my code account for that? If the value is later moved i have to own it which
                //might change the relation to (for example) the variable refernece (like turning a
                //reference lookup into a copy)
                val initialValue = instruction.initialValue?.let { processExpression(it, BORROW) }
                if (instruction.declaredType != null && initialValue != null) {
                    val typeMatch = instruction.declaredType.acceptsInstanceOf(initialValue.type)
                    if (!typeMatch.value)
                        messages.error(typeMatch, instruction.initialValue.location)
                }
                val variableState = ExecutionState.VariableState(
                    declaredType = instruction.declaredType,
                    inferredType = initialValue?.type ?: run {
                        if (instruction.declaredType == null) messages.error(
                            "Variable does not have definitive type. Either specify type or add intitial value for inference",
                            instruction.name
                        )
                        null
                    },
                    //Why do we own if the value is borrowed??
                    ownershipState = Ownership.OWNS,
                    //This should be intrgrated an taken from initialValue
                    readsExternal = ReasonedBoolean.False("probably wrong"),
                    possibleValues = listOfNotNull(initialValue),
                    declaration = instruction
                )
                instruction.implicitType = variableState.inferredType
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
        val returnValueExpression = instruction.value
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
        val actualType = returnValueExpression.calculateType(
            executionState.localVariables.mapValues { it.value.type },
            messages
        ) ?: Type.Undefined
        val typeMatch = expectedReturnType.acceptsInstanceOf(actualType)
        if (!typeMatch.value)
            messages.error(typeMatch, returnValueExpression.location)
    }

    private fun processExpression(expression: Expression, ownershipType: PassingType): ValueInfo {
        if (expression is ScopeAware) expression.setEnclosingScope(executionState.asScope())
        if (expression is Access<*>) expression.finalize(messages)
        if (expression is ScopeProvider) expression.setScopes()
        val localVarTypes = executionState.localVariables.mapValues { it.value.type }
        return when (expression) {
            is Constant<*> -> ValueInfo(ReasonedBoolean.True("Constants are deterministic"), expression.type)
            is Instruction.FunctionCall ->
                processFunctionCall(expression) ?: run {
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
                    type = expression.calculateType(localVarTypes, messages) ?: Type.Undefined,
                    affectedBy = left.affectedBy + right.affectedBy
                )
            }

            is Expression.Undefined -> ValueInfo(
                ReasonedBoolean.False("Undefined expressions are not deterministic"),
                Type.Undefined
            )

            is Expression.VariableReference -> {
                val localVariableInfo = executionState.localVariables[expression.reference.value]
                if (localVariableInfo?.ownershipState == Ownership.MOVED) {
                    messages.error(
                        "Variable ${expression.reference.value} is moved and cannot be used anymore",
                        expression.reference.location
                    )
                }
                if (ownershipType == MOVE) moveVariable(expression.reference)
                val possibleValues = localVariableInfo?.possibleValues?.toTypedArray() ?: arrayOf()
                ValueInfo(
                    deterministic = possibleValues.map { it.deterministic }.reduceRightOrNull { a, b -> a.and(b) }
                        ?: ReasonedBoolean.False("Variable has no possible values"),
                    type = localVariableInfo?.type ?: Type.Undefined
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
        //atm this is wrong, currently function parameters are not tracked in execution state.localVariables
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
        functionCall.setEnclosingScope(executionState.asScope())
        functionCall.calculateType(localTypes, messages)
        val target = functionCall.target
        if (target == null) {
            return ValueInfo(
                ReasonedBoolean.False("Function not found"),
                Type.Undefined
            );
            //throw IllegalStateException("Function ${functionCall.reference.value} not resolved")
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
    return when(body) {
        is Function.Body.Implementation -> analyzeImplementation(body, messages)
        Function.Body.Missing -> TODO()
        Function.Body.Native -> nativeFunctionInformation()
    }
}

private fun Function.analyzeImplementation(
    body: Function.Body.Implementation,
    messages: CompilerMessages
): FunctionInfo {
    val analyzer = CodeBlockAnalysis(body.block, this.returnType, messages, scope)
    analyzer.executionState.localVariables.putAll(parameters.associate {
        it.name.value to ExecutionState.VariableState(
            declaredType = it.declaredType,
            inferredType = null,
            ownershipState = Ownership.BORROWS,
            possibleValues = listOf(
                ValueInfo(
                    deterministic = ReasonedBoolean.True("Parameters are per definiton deterministic"),
                    affectedBy = listOf(),
                    type = it.declaredType
                )
            ),
            readsExternal = ReasonedBoolean.False("Parameters are not external state"),
            declaration = it
        )
    })
    analyzer.analyze()
    return analyzer.toInfo()
}

private fun Function.nativeFunctionInformation(): FunctionInfo = FunctionInfo(
    calls = listOf(),
    returnInfo = ValueInfo(
        deterministic = ReasonedBoolean.True("TODO!!: Native functions must be annotated with deterministic/non-deterministic"),
        type = returnType!!,
        affectedBy = parameters.map {
            ValueInfo(
                deterministic = ReasonedBoolean.True("Parameters are deterministic by definition"),
                type = it.declaredType,
                affectedBy = emptyList()
            )
        }
    ),
    writeExternalState = ReasonedBoolean.False("TODO!!: Native functions must be annotated with write-external state info"),
    parameters = parameters.associate {
        it.name.value to ParameterInfo(
            passingType = BORROW,//TODO!! figure out how to determine native function passing
            affectsOutput = true
        )
    }
)

