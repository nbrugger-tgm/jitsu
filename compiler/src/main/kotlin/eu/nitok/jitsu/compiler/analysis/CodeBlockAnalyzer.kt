package eu.nitok.jitsu.compiler.analysis

import eu.nitok.jitsu.common.CompilerMessage
import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.compiler.graph.*
import eu.nitok.jitsu.compiler.graph.Function
import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.Located
import eu.nitok.jitsu.common.ReasonedBoolean.*

class CodeBlockAnalyzer(
    private val function: Function,
    private val calleeOracle: (Function) -> FunctionSummary?,
    private val messages: CompilerMessages
) {
    data class AnalysisResult(
        val functionSummary: FunctionSummary,
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
    )

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

    private data class ReturnPathInfo(
        val type: Type,
        val compileTimeValue: AbstractValue,
        val paramDeps: Set<String>,
        val deterministic: ReasonedBoolean
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

        val returnSummary: ReturnSummary? = if (returnPaths.isEmpty()) {
            null
        } else {
            returnPaths.reduce { acc, path ->
                ReturnPathInfo(
                    type = path.type,//What is this needed for an if it is needed why aren't the types merged?
                    compileTimeValue = acc.compileTimeValue.join(path.compileTimeValue),
                    paramDeps = acc.paramDeps + path.paramDeps,
                    deterministic = acc.deterministic.and(path.deterministic)
                )
            }.let { merged ->
                ReturnSummary(
                    possibleTypes = returnPaths.map { rp -> rp.type }.distinct(),
                    compileTimeValue = merged.compileTimeValue,
                    dependsOnParameters = merged.paramDeps
                        .filter { dep -> fnParams.any { p -> p.name.value == dep } }
                        .toSet(),//Why is this needed? what else besides parameters is in this stream
                    deterministic = merged.deterministic
                )
            }
        }

        val variableSummaries = mutableMapOf<String, VariableSummary>()

        for (param in function.parameters) {
            val state = localVariables[param.name.value]
            if (state != null) {
                variableSummaries[param.name.value] = buildVariableSummary(state)
            }
        }

        for ((_, state) in localVariables) {
            if (fnParams.none { p -> p.name.value == state.variable.name.value }) {
                variableSummaries[state.variable.name.value] = buildVariableSummary(state)
            }
        }
        val functionSummary = FunctionSummary(
            noSideEffects = hasSideEffects,
            parameterModes = parameterModes,
            returnSummary = returnSummary,
            callees = callees.distinct(),
            variableSummary = variableSummaries
        )


        return AnalysisResult(
            functionSummary = functionSummary,
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
        decl.implicitType = expression?.type
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
                paramDeps = exprResult.paramDeps,
                deterministic = exprResult.deterministic
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
                deterministic = False("Undefined expression"),
                constValue = AbstractValue.Unknown,
                paramDeps = emptySet()
            )
            is Expression.ArrayLiteral -> ExpressionResult(
                type = expr.calculateType(typeContext, messages)?:Type.Array(Type.Undefined, null),
                deterministic = expr.elements.asSequence().map { analyzeExpression(it).deterministic }.fold(
                    ReasonedBoolean.True("Empty array is deterministic") as ReasonedBoolean
                ) { acc, d -> acc.and(d) },
                constValue = AbstractValue.Unknown,
                paramDeps = expr.elements.asSequence().flatMap { analyzeExpression(it).paramDeps }.toSet()
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
        val call = op.asFunctionCall()
        val analyzedFunction = analyzeFunctionCall(call)
        op.target = call.target
        if(call.target != null) op.type = call.type
        return analyzedFunction
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
        val type = ref.calculateType(typeContext, messages)?: Type.Undefined

        useSiteInfos[ref] = UseSiteInfo(
            narrowedType = type,
            ownershipState = varState.ownershipState
        )

        return ExpressionResult(
            type = type,
            deterministic = varState.deterministic,
            constValue = varState.compileTimeValue,
            paramDeps = varState.paramDeps
        )
    }

    private fun analyzeFunctionCall(call: Instruction.FunctionCall): ExpressionResult {
        val argumentExpressions = call.callParameters.map { analyzeExpression(it) }

        val target = resolveFunctionCall(call, argumentExpressions)

        val outputInfluencingParams = target?.summary?.let { targetSummary ->
            targetSummary.returnSummary?.dependsOnParameters
                ?.map { paramName -> target.parameters.indexOfFirst { it.name.value == paramName } }
        } ?: argumentExpressions.withIndex().map { it.index } //if target doesn't exist assume all parameters to be return value influencing

        val areOutputInfluencingArgsDeterministic = argumentExpressions.asSequence()
            .filterIndexed { index, _ -> outputInfluencingParams.contains(index) }
            .fold(ReasonedBoolean.True("No arguments") as ReasonedBoolean) { acc, arg ->
                acc.and(arg.deterministic)
            }
        val allParamDeps = argumentExpressions.asSequence()
            .filterIndexed { index, _ -> outputInfluencingParams.contains(index) }
            .flatMap { it.paramDeps }
            .toSet()
        val returnType = call.calculateType(typeContext, messages) ?: run {
            messages.error("Expected value but $call calls void function", call.location)
            Type.Undefined
        }
        val targetSummary = target?.let{ calleeOracle(it) }
        val deterministic = targetSummary?.returnSummary?.deterministic?.and(areOutputInfluencingArgsDeterministic)?:areOutputInfluencingArgsDeterministic

        return if (target != null) {
            if (targetSummary != null) {
                if (!targetSummary.noSideEffects.value) {
                    hasSideEffects = hasSideEffects.and(
                        False(
                            "'${call.reference.value}' has side effects",
                            listOf(CompilerMessage.Hint("${call.reference.value} is called here", call.location)),
                            listOf(null to targetSummary.noSideEffects)
                        )
                    )
                }
            } else {
                hasSideEffects = hasSideEffects.and(
                    ReasonedBoolean.False("Target '${call.reference.value}' has no known summary")
                )
            }
            ExpressionResult(
                type = returnType,
                deterministic = deterministic,
                constValue = targetSummary?.returnSummary?.compileTimeValue?:AbstractValue.Unknown,
                paramDeps = allParamDeps
            )
        } else {
            hasSideEffects = hasSideEffects.and(
                ReasonedBoolean.False("Unresolved target '${call.reference.value}'")
            )
            ExpressionResult(
                type = returnType,
                deterministic = ReasonedBoolean.True("Unresolved target '${call.reference.value}'"),
                constValue = AbstractValue.Unknown,
                paramDeps = allParamDeps
            )
        }
    }

    private fun resolveFunctionCall(
        call: Instruction.FunctionCall,
        argResults: List<ExpressionResult>
    ): Function? {
        if (call.target != null) {
            return call.target
        }
        val argumentTypes = argResults.mapIndexed { i, result ->
            Located(result.type, call.callParameters[i].location)
        }.toTypedArray()
        val resolvedTarget = call.scope.resolveFunction(call.reference, argumentTypes, messages)
        if (resolvedTarget != null) {
            call.target = resolvedTarget
            resolvedTarget.accessToSelf.add(call)
        }
        return resolvedTarget;
    }

    private val typeContext: Map<String, Type> get() = localVariables.mapValues { it.value.narrowedType }

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
            compileTimeValue = state.compileTimeValue,
            ownershipState = state.ownershipState
        )
    }
}
