package eu.nitok.jitsu.compiler.analysis

import eu.nitok.jitsu.compiler.analysis.ParameterInfo.OwnershipType.*
import eu.nitok.jitsu.compiler.ast.CompilerMessages
import eu.nitok.jitsu.compiler.ast.Located
import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage
import eu.nitok.jitsu.compiler.graph.*
import eu.nitok.jitsu.compiler.graph.Function
import kotlinx.serialization.Serializable


@Serializable
data class ParameterInfo(
    val ownershipType: OwnershipType,
    val affectsOutput: Boolean
) {
    enum class OwnershipType {
        /**
         * The parameter does not leave the scope and the callee can still use/manage the variable afterward
         */
        BORROW,

        /**
         * The parameter needs to be managed by the function and the callee needs to either give controll to the function
         * or hand the function a copy.
         */
        MOVE
    }
}

@Serializable
data class ValueInfo(
    /**
     * Deterministic means that the function produces the same output given the same input. Where with methods `this`
     * counts as input
     */
    val deterministic: Boolean
) {
    fun merge(otherValues: Iterable<ValueInfo>): ValueInfo {
        return ValueInfo(deterministic && otherValues.all { it.deterministic })
    }
}

@Serializable
data class FunctionInfo(
    val calls: List<FunctionInfo>,
    val returnInfo: ValueInfo?,
    val writeExternalState: Boolean = false,
    val parameters: Map<String, ParameterInfo>
) {
    val hasSideEffects: Boolean get() = writeExternalState || calls.none { it.hasSideEffects }
}

data class ExecutionState(
    val localFunctions: MutableMap<String, MutableList<Type.FunctionTypeSignature>> = mutableMapOf(),
    val localVariables: MutableMap<String, VariableState> = mutableMapOf()
) {
    data class VariableState(
        val declaredType: Type?,
        var inferredType: Type,
        var ownershipType: ParameterInfo.OwnershipType,
        var readsExternal: Boolean,
        val possibleValues: List<ValueInfo>
    )
}

class FunctionAnalyzer(val fn: Function, val messages: CompilerMessages) {

    var returnInfo: ValueInfo? = null

    val calls: MutableList<FunctionInfo> = mutableListOf()
    val writeExternalState: Boolean = false;
    val parameters: MutableMap<String, ParameterInfo.OwnershipType> = mutableMapOf()
    val executionState = ExecutionState()
    val parentScope = fn.body.scope.parent ?: throw IllegalStateException("Function scope has no parent")


    fun analyze() {
        fn.parameters.forEach { parameters[it.name.value] = BORROW }
        for (instruction in fn.body.instructions) {
            processInstruction(instruction)
        }
    }

    private fun processInstruction(instruction: Instruction) {
        when (instruction) {
            is Function -> {
                instruction.name?.let {
                    executionState.localFunctions.computeIfAbsent(it.value) { mutableListOf() }
                        .add(instruction.signature)
                }
            }

            is Instruction.FunctionCall -> processFunctionCall(instruction)

            is Instruction.Return -> {
                returnInfo = instruction.value?.let { processExpression(it, MOVE) }
            }

            is Instruction.VariableDeclaration -> {
                val inferredType = instruction.value.implicitType ?: Type.Undefined
                val initialValue = processExpression(instruction.value, BORROW)
                val variableState = ExecutionState.VariableState(
                    declaredType = instruction.variable.declaredType,
                    inferredType = inferredType,
                    ownershipType = BORROW,
                    readsExternal = false,
                    possibleValues = listOfNotNull(initialValue)
                )
                val varName = instruction.variable.name
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

    private fun processExpression(value: Expression, ownershipType: ParameterInfo.OwnershipType): ValueInfo {
        return when (value) {
            is Constant<*> -> ValueInfo(true)
            is Instruction.FunctionCall -> processFunctionCall(value)?: run {
                messages.error(
                    "Function ${value.reference.value} does not have a return type and cannot be used as value",
                    value.reference.location
                )
                return ValueInfo(true)
            }

            is Expression.Operation -> {
                val left = processExpression(value.left, BORROW)
                val right = processExpression(value.right, BORROW)
                left.merge(listOf(right))
            }

            Expression.Undefined -> ValueInfo(true)
            is Expression.VariableReference -> {
                val variableInfo = executionState.localVariables[value.reference.value]
                if (variableInfo?.ownershipType == MOVE) {
                    messages.error(
                        "Variable ${value.reference.value} is moved and cannot be used anymore",
                        value.reference.location
                    )
                }
                if (ownershipType == MOVE) moveVariable(value.reference)
                variableInfo?.possibleValues?.firstOrNull()?.merge(variableInfo.possibleValues.drop(1)) ?: ValueInfo(
                    true
                )
            }
        }
    }


    fun moveVariable(varRef: Located<String>) {
        val parameter = fn.parameters.find { it.name.value == varRef.value }
        if (parameter != null) {
            parameters[parameter.name.value] = MOVE
            return;
        }
        val variable = executionState.localVariables[varRef.value]
        if (variable != null) {
            variable.ownershipType = MOVE
            return
        }
        messages.error(
            "Moving global is forbidden (wip)",
            varRef.location
        )
    }

    fun processFunctionCall(functionCall: Instruction.FunctionCall):ValueInfo? {
        val isNotGlobal = parentScope.allFunctions[functionCall.reference.value].isNullOrEmpty()
        if (functionCall.target != null && isNotGlobal) {
            messages.error(
                "Function ${functionCall.reference.value} not defined yet!",
                functionCall.reference.location,
                CompilerMessage.Hint(
                    "nested functions are not hoisted, so a function needs to be defined before it is called",
                    functionCall.target!!.name!!.location
                )
            )
            return ValueInfo(true)
        } else if (functionCall.target == null) {
            return ValueInfo(true)
        }
        val calledFunction = functionCall.target!!.info(messages)
        val parameterValueInfos = calledFunction.parameters.mapValues { (a, b) ->
            functionCall.parameters[a]?.let { processExpression(it, b.ownershipType) }?: run {
                messages.error(
                    "Parameter $a is not provided",
                    functionCall.reference.location
                )
                ValueInfo(true)
            }
        }
        val outputInfluencingParameters = calledFunction.parameters.filter { it.value.affectsOutput }.keys
        val outputInfluencingValueInfos = parameterValueInfos
            .filter { it.key in outputInfluencingParameters }
            .values.toList()
        val targetReturnInfo = calledFunction.returnInfo ?: return null
        calls.add(calledFunction)
        return targetReturnInfo.merge(outputInfluencingValueInfos)
    }


    fun toInfo(): FunctionInfo {
        return FunctionInfo(
            calls,
            returnInfo,
            writeExternalState,
            parameters.mapValues { ParameterInfo(it.value, true) }
        )
    }
}

fun Function.calculateFunctionInfo(messages: CompilerMessages): FunctionInfo {
    val analyzer = FunctionAnalyzer(this, messages)
    analyzer.analyze()
    return analyzer.toInfo()
}
