package eu.nitok.jitsu.compiler.analysis

import eu.nitok.jitsu.common.CompilerMessage
import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.compiler.graph.*
import eu.nitok.jitsu.compiler.graph.Function
import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.Located

class CodeBlockAnalyzer(
    private val function: Function,
    private val calleeOracle: (Function) -> FunctionSummary?,
    private val messages: CompilerMessages
) {
    data class AnalysisResult(
        val functionSummary: FunctionSummary,
        val variableSummaries: Map<Variable, VariableSummary>,
        val useSiteInfos: Map<Expression.VariableReference, UseSiteInfo>
    )

    private data class VariableState(
        val declaredType: Type?,
        var narrowedType: Type,
        var ownershipState: OwnershipState,
        var isConstant: Boolean,
        var compileTimeValue: AbstractValue,
        var paramDeps: Set<String>,
        val variable: Variable,
        val deterministic: ReasonedBoolean
    ) {
    }

    private data class ExpressionResult(
        val type: Type,
        val deterministic: ReasonedBoolean,
        val constValue: AbstractValue,
        val paramDeps: Set<String>
    )

    private val localVariables: MutableMap<String, VariableState> = mutableMapOf()
    private val useSiteInfos: MutableMap<Expression.VariableReference, UseSiteInfo> = mutableMapOf()
    private val returnPaths: MutableList<ReturnPathInfo> = mutableListOf()
    private val callees: MutableList<Function> = mutableListOf()
    private var hasSideEffects: ReasonedBoolean = ReasonedBoolean.True("No side effects observed yet")
    private var isDeterministic: ReasonedBoolean = ReasonedBoolean.True("No non-determinism observed yet")

    private data class ReturnPathInfo(
        val type: Type,
        val compileTimeValue: AbstractValue,
        val paramDeps: Set<String>
    )

    fun analyze(): AnalysisResult {
         for (param in function.parameters) {
            localVariables[param.name.value] = VariableState(
                declaredType = param.declaredType,
                narrowedType = param.declaredType,
                ownershipState = OwnershipState.BORROWS,
                isConstant = false,
                compileTimeValue = AbstractValue.Unknown,
                paramDeps = setOf(param.name.value),
                variable = param,
                deterministic = ReasonedBoolean.True("Parameters are deterministic")
            )
        }
        if(function.body is Function.Body.Implementation) {
            for (instruction in function.body.block.instructions) {
                processInstruction(instruction)
            }
        }

        val parameterModes: Map<String, ParameterMode> = function.parameters.associate { param: Function.Parameter ->
            val hasMoveAccess = (param.accessToSelf as List<*>)
                .filterIsInstance<Access.VariableAccess>()
                .any { access -> access.accessKind == Access.VariableAccess.AccessKind.MOVE }
            param.name.value to if (hasMoveAccess) ParameterMode.MOVE else ParameterMode.BORROW
        }

        val fnParams = function.parameters
        val parameterInfluence: Set<String> = returnPaths
            .asSequence()
            .flatMap { path -> path.paramDeps }
            .filter { dep -> fnParams.any { p -> p.name.value == dep } }//Why is this needed? what else besides parameters is in this stream
            .toSet()

        val returnSummary: ReturnSummary? = if (returnPaths.isEmpty()) {
            null
        } else {
            returnPaths.reduce { acc, path ->
                ReturnPathInfo(
                    type = path.type,//What is this needed for an if it is needed why aren't the types merged?
                    compileTimeValue = acc.compileTimeValue.join(path.compileTimeValue),
                    paramDeps = acc.paramDeps + path.paramDeps
                )
            }.let { merged ->
                ReturnSummary(
                    possibleTypes = returnPaths.map { rp -> rp.type }.distinct(),
                    compileTimeValue = merged.compileTimeValue,
                    dependsOnParameters = merged.paramDeps
                        .filter { dep -> fnParams.any { p -> p.name.value == dep } }
                        .toSet()
                )
            }
        }

        val functionSummary = FunctionSummary(
            deterministic = isDeterministic,
            noSideEffects = hasSideEffects,
            parameterModes = parameterModes,
            parameterInfluence = parameterInfluence,//Why do we have parameterInfluence and returnSummary.dependsOnParameters seems redundant
            returnSummary = returnSummary,
            callees = callees.distinct()//string does not suffice here? How would that deal with overloads
        )

        val variableSummaries = mutableMapOf<Variable, VariableSummary>()

        for (param in function.parameters) {
            val state = localVariables[param.name.value]
            if (state != null) {
                variableSummaries[param] = buildVariableSummary(state)
            }
        }

        for ((_, state) in localVariables) {
            if (fnParams.none { p -> p.name.value == state.variable.name.value }) {
                variableSummaries[state.variable] = buildVariableSummary(state)
            }
        }

        return AnalysisResult(
            functionSummary = functionSummary,
            variableSummaries = variableSummaries,
            useSiteInfos = useSiteInfos
        )
    }

    private fun processInstruction(instruction: Instruction) {
        when (instruction) {
            is VariableDeclaration -> processVariableDeclaration(instruction)
            is Instruction.FunctionCall -> processFunctionCallInstruction(instruction)
            is Instruction.Return -> processReturn(instruction)
            is Function -> TODO()
        }
    }

    private fun processVariableDeclaration(decl: VariableDeclaration) {
        val expression: ExpressionResult? = decl.initialValue?.let { analyzeExpression(it) }

        var narrowedType: Type? = when {
            expression != null -> expression.type
            decl.declaredType != null -> decl.declaredType
            else -> null
        }
        if (expression != null && decl.declaredType != null) {
            val assignedType = expression.type
            val initialTypeValid = decl.declaredType.acceptsInstanceOf(assignedType)
            if (!initialTypeValid.value) {
                messages.error(initialTypeValid, decl.initialValue.location)
            }
            narrowedType = decl.declaredType
        }

        val isConstant = !decl.reassignable && expression?.constValue is AbstractValue.Const

        val compileTimeValue: AbstractValue = expression?.constValue ?: AbstractValue.NoValue

        val paramDeps: Set<String> = expression?.paramDeps ?: emptySet()

        localVariables[decl.name.value] = VariableState(
            declaredType = decl.declaredType ?: narrowedType,
            narrowedType = narrowedType ?: decl.declaredType ?: Type.Undefined,
            ownershipState = OwnershipState.OWNS,
            isConstant = isConstant,
            compileTimeValue = compileTimeValue,
            paramDeps = paramDeps,
            variable = decl,
            deterministic = expression?.deterministic ?: ReasonedBoolean.True("Variable not assigned yet")
        )
    }

    private fun processFunctionCallInstruction(call: Instruction.FunctionCall) {
        analyzeExpression(call)
    }

    private fun processReturn(ret: Instruction.Return) {
        val value = ret.value

        if (value == null) {
            if (function.returnType != null) {
                messages.error(
                    "Function expects return value of type ${function.returnType}",
                    ret.location,
                    CompilerMessage.Hint("Return type defined here", function.returnType.location)
                )
            }
            return
        }

        val exprResult = analyzeExpression(value)

        if (function.returnType != null) {
            val typeMatches = function.returnType.value.acceptsInstanceOf(exprResult.type)
            if (!typeMatches.value) {
                messages.error("Function expects return type ${function.returnType.value}: ${typeMatches.fullMessageChain()}", value.location,
                    CompilerMessage.Hint("Return type defined here", function.returnType.location)
                )
            }
        } else {
            messages.error(
                "Returning value from non-return function", value.location,
                *(function.name?.location?.let {
                    arrayOf(CompilerMessage.Hint("Define return type for this function ': <return type>'", it))
                } ?: arrayOf())
            )
            return;
        }

        returnPaths.add(
            ReturnPathInfo(
                type = exprResult.type,
                compileTimeValue = exprResult.constValue,
                paramDeps = exprResult.paramDeps
            )
        )
    }

    private fun analyzeExpression(expr: Expression): ExpressionResult {
        return when (expr) {
            is Constant<*> -> analyzeConstant(expr)
            is Expression.Operation -> analyzeOperation(expr)
            is Expression.VariableReference -> analyzeVariableReference(expr)
            is Instruction.FunctionCall -> analyzeFunctionCall(expr)
            is Expression.Undefined -> ExpressionResult(
                type = Type.Undefined,
                deterministic = ReasonedBoolean.False("Undefined expression"),
                constValue = AbstractValue.Unknown,
                paramDeps = emptySet()
            )
        }
    }

    private fun analyzeConstant(constant: Constant<*>): ExpressionResult {
        val type = constant.type
        return ExpressionResult(
            type = type,
            deterministic = ReasonedBoolean.True("Constant values are deterministic"),
            constValue = AbstractValue.Const(constant.literal, type),
            paramDeps = emptySet()
        )
    }

    private fun analyzeOperation(op: Expression.Operation): ExpressionResult {
        val left = analyzeExpression(op.left)
        val right = analyzeExpression(op.right)

        val deterministic = left.deterministic.and(right.deterministic)
        val type = op.calculateType(typeContext, messages) ?: Type.Undefined

        val constValue: AbstractValue =
            AbstractValue.Unknown //If both are constant this can be constant as well in the future

        return ExpressionResult(
            type = type,
            deterministic = deterministic,
            constValue = constValue,
            paramDeps = left.paramDeps + right.paramDeps
        )
    }

    private fun analyzeVariableReference(ref: Expression.VariableReference): ExpressionResult {
        val varName = ref.reference.value
        val varState = localVariables[varName]

        if (varState == null) {
            //Todo: global variable access
            messages.error("Variable '$varName' not found", ref.location)
            useSiteInfos[ref] = UseSiteInfo(
                narrowedType = Type.Undefined,
                ownershipState = OwnershipState.BORROWS
            )
            return ExpressionResult(
                type = Type.Undefined,
                deterministic = ReasonedBoolean.True("Variable '$varName' not found"),
                constValue = AbstractValue.NoValue,
                paramDeps = emptySet()
            )
        }

        ref.target = varState.variable
        ref.accessKind = Access.VariableAccess.AccessKind.BORROW
        varState.variable.accessToSelf.add(ref)

        useSiteInfos[ref] = UseSiteInfo(
            narrowedType = varState.narrowedType,
            ownershipState = varState.ownershipState
        )

        return ExpressionResult(
            type = varState.narrowedType,
            deterministic = varState.deterministic,
            constValue = varState.compileTimeValue,
            paramDeps = varState.paramDeps
        )
    }

    private fun analyzeFunctionCall(call: Instruction.FunctionCall): ExpressionResult {
        val argResults = call.callParameters.map { analyzeExpression(it) }

        if (call.target == null) {
            val parameterTypes = argResults.mapIndexed { i, result ->
                Located(result.type, call.callParameters[i].location)
            }.toTypedArray()
            val resolvedTarget = call.scope.resolveFunction(call.reference, parameterTypes, messages)
            if (resolvedTarget != null) {
                call.target = resolvedTarget
                resolvedTarget.accessToSelf.add(call)
            }
        }
        val target = call.target
        if (target != null) callees.add(target)

        val outputInfluencingParams = call.target?.summary?.parameterInfluence
            ?.map { paramName -> call.target?.parameters?.indexOfFirst { it.name.value == paramName } }
            ?.filterNotNull() ?: listOf()

        val areOutputInfluencingArgsDeterministic = argResults.asSequence()
            .filterIndexed { index, _ -> outputInfluencingParams.contains(index) }
            .fold(ReasonedBoolean.True("No arguments") as ReasonedBoolean) { acc, arg ->
                acc.and(arg.deterministic)
            }
        val allParamDeps = argResults.asSequence()
            .filterIndexed { index, _ -> outputInfluencingParams.contains(index) }
            .flatMap { it.paramDeps }
            .toSet()

        return if (target != null) {
            val targetSummary = calleeOracle(target)
            if (targetSummary != null) {
                val deterministic = targetSummary.deterministic.and(areOutputInfluencingArgsDeterministic)

                if (!targetSummary.noSideEffects.value) {
                    hasSideEffects = hasSideEffects.and(
                        ReasonedBoolean.False(
                            "'${call.reference.value}' has side effects",
                            listOf(CompilerMessage.Hint("${call.reference.value} is called here", call.location)),
                            listOf(null to targetSummary.noSideEffects)
                        )
                    )
                }
                if (!targetSummary.deterministic.value) {
                    //TODO: Methods inside a deterministic method can be non-deterministic as long as the return value is not influenced by the call result
                    isDeterministic = isDeterministic.and(
                        ReasonedBoolean.False(
                            "'${call.reference.value}' is non-deterministic",
                            listOf(CompilerMessage.Hint("${call.reference.value} is called here", call.location)),
                            listOf(null to targetSummary.deterministic)
                        )
                    )
                }

                val returnType = target.returnType?.value ?: Type.Null

                ExpressionResult(
                    type = returnType,
                    deterministic = deterministic,
                    constValue = AbstractValue.Unknown,
                    paramDeps = allParamDeps
                )
            } else {
                hasSideEffects = hasSideEffects.and(
                    ReasonedBoolean.False("Target '${call.reference.value}' has no known summary")
                )
                isDeterministic = isDeterministic.and(
                    ReasonedBoolean.True("Target '${call.reference.value}' has no known summary")
                )

                val returnType = call.calculateType(typeContext, messages) ?: Type.Undefined
                ExpressionResult(
                    type = returnType,
                    deterministic = ReasonedBoolean.True("Unknown target '${call.reference.value}'"),
                    constValue = AbstractValue.Unknown,
                    paramDeps = allParamDeps
                )
            }
        } else {
            hasSideEffects = hasSideEffects.and(
                ReasonedBoolean.False("Unresolved target '${call.reference.value}'")
            )
            isDeterministic = isDeterministic.and(
                ReasonedBoolean.True("Unresolved target '${call.reference.value}'")
            )

            val returnType = call.calculateType(typeContext, messages) ?: Type.Undefined
            ExpressionResult(
                type = returnType,
                deterministic = ReasonedBoolean.True("Unresolved target '${call.reference.value}'"),
                constValue = AbstractValue.Unknown,
                paramDeps = allParamDeps
            )
        }
    }

    private val typeContext: Map<String, Type> = localVariables.mapValues { it.value.narrowedType }

    private fun buildVariableSummary(state: VariableState): VariableSummary {
        val effectivelyConstant: ReasonedBoolean =
            if (state.isConstant) {//Cant is constant be turned into a ReasonedBoolean itself and therefore be more percise
                ReasonedBoolean.True("Variable is not reassignable and its initializer is a compile-time constant")
            } else {
                ReasonedBoolean.False("Variable is either reassignable or its initializer is not a compile-time constant")
            }
        return VariableSummary(
            declaredType = state.declaredType,
            narrowedType = state.narrowedType,
            effectivelyConstant = effectivelyConstant,
            compileTimeValue = state.compileTimeValue
        )
    }
}
