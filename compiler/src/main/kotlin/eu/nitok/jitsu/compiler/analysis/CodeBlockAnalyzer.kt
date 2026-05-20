package eu.nitok.jitsu.compiler.analysis

import eu.nitok.jitsu.common.CompilerMessage
import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.common.ReasonedBoolean.False
import eu.nitok.jitsu.common.ReasonedBoolean.True
import eu.nitok.jitsu.compiler.graph.*
import eu.nitok.jitsu.compiler.graph.Function
import eu.nitok.jitsu.compiler.graph.Access

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
    private var hasSideEffects: ReasonedBoolean = True("No side effects observed yet")

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
                deterministic = True("Parameters are deterministic")
            )
        }
        if (function.body is Function.Body.Implementation) {
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
                        .filter { dep -> fnParams.any { p -> p.name.value == dep } }//Why is this needed? what else besides parameters is in this stream
                        .toSet(),
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
            is Instruction.FunctionCall -> processFunctionCall(instruction)
            is Instruction.Return -> processReturn(instruction)
            is Function -> TODO("nested functions not implemented yet")
        }
    }

    private fun processVariableDeclaration(decl: VariableDeclaration) {
        val expression: ExpressionResult? = decl.initialValue?.let { analyzeExpression(it, decl.declaredType?.rawType(messages)) }
        decl.implicitType = expression?.type
        val narrowedType: Type? = when {
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
            deterministic = expression?.deterministic ?: True("Variable not assigned yet")
        )
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

        val exprResult = analyzeExpression(value, function.returnType?.value)

        if (function.returnType != null) {
            val typeMatches = function.returnType.value.acceptsInstanceOf(exprResult.type)
            if (!typeMatches.value) {
                messages.error(
                    "Function expects return type ${function.returnType.value}: ${typeMatches.fullMessageChain()}",
                    value.location,
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

    private fun analyzeExpression(expr: Expression, typeHint: Type?): ExpressionResult {
        return when (expr) {
            is Constant<*> -> analyzeConstant(expr)
            is Expression.VariableReference -> analyzeVariableReference(expr)
            is Instruction.FunctionCall -> analyzeFunctionCallExpression(expr)
            is Expression.Undefined -> ExpressionResult(
                type = Type.Undefined,
                deterministic = False("Undefined expression"),
                constValue = AbstractValue.Unknown,
                paramDeps = emptySet()
            )

            is Expression.ArrayLiteral -> analyzeArrayLiteral(expr, typeHint)
        }
    }

    private fun analyzeArrayLiteral(expr: Expression.ArrayLiteral, typeHint: Type?): ExpressionResult {
        val arrayType = expr.calculateType(typeContext, messages, typeHint)
        val elementType = if (arrayType.elementType !is Type.Undefined) arrayType.elementType else null
        val elements = expr.elements.asSequence().map { analyzeExpression(it, elementType) }.toList()
        return ExpressionResult(
            type = arrayType,
            deterministic = elements.map { it.deterministic }.fold(
                True("Empty array is deterministic") as ReasonedBoolean
            ) { acc, d -> acc.and(d) },
            constValue = AbstractValue.Unknown,
            paramDeps = elements.flatMap { it.paramDeps }.toSet()
        )
    }

    private fun analyzeConstant(constant: Constant<*>): ExpressionResult {
        val type = constant.type
        return ExpressionResult(
            type = type,
            deterministic = True("Constant values are deterministic"),
            constValue = AbstractValue.Const(constant.literal, type),
            paramDeps = emptySet()
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
                deterministic = True("Variable '$varName' not found"),
                constValue = AbstractValue.NoValue,
                paramDeps = emptySet()
            )
        }

        ref.setResolvedTarget(varState.variable)
        ref.accessKind = Access.VariableAccess.AccessKind.BORROW
        varState.variable.accessToSelf.add(ref)
        val type = ref.calculateType(typeContext, messages) ?: Type.Undefined

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

    private fun processFunctionCall(call: Instruction.FunctionCall): Function? {
        val target = call.target ?: call.resolveTarget(typeContext, messages)
        val targetSummary = target?.let { calleeOracle(it) }
        if (target != null) {
            call.setResolvedTarget(target)
            callees.add(target)
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
        } else {
            hasSideEffects = hasSideEffects.and(
                ReasonedBoolean.False("Unresolved target '${call.reference.value}'")
            )
        }
        return target
    }

    private fun analyzeFunctionCallExpression(call: Instruction.FunctionCall): ExpressionResult {
        val target = processFunctionCall(call)
        val targetSummary = target?.let { calleeOracle(it) }
        val argumentExpressions = call.callParameters.mapIndexed { index, expression ->
            if (target != null) analyzeExpression(expression, target.parameters.getOrNull(index)?.type)
            else analyzeExpression(expression, null)
        }


        val outputInfluencingParams = target?.summary?.let { targetSummary ->
            targetSummary.returnSummary?.dependsOnParameters
                ?.map { paramName -> target.parameters.indexOfFirst { it.name.value == paramName } }
        } ?: argumentExpressions.withIndex()
            .map { it.index } //if target doesn't exist assume all parameters to be return value influencing

        val areOutputInfluencingArgsDeterministic = argumentExpressions.asSequence()
            .filterIndexed { index, _ -> outputInfluencingParams.contains(index) }
            .fold(True("No arguments") as ReasonedBoolean) { acc, arg ->
                acc.and(arg.deterministic)
            }
        val allParamDeps = argumentExpressions.asSequence()
            .filterIndexed { index, _ -> outputInfluencingParams.contains(index) }
            .flatMap { it.paramDeps }
            .toSet()
        val returnType = call.calculateType(typeContext, messages) ?: run {
            if(target != null) messages.error("Expected value but ${call.reference.value} is a void function", call.location)
            Type.Undefined
        }
        val targetDeterminsitic =
            if (targetSummary?.returnSummary?.deterministic == null) True("Unresolved functions are assumed to be deterministic")
            else if (targetSummary.returnSummary.deterministic.value) True("$target is deterministic")
            else False("$target is not deterministic", targetSummary.returnSummary.deterministic)
        val deterministic = areOutputInfluencingArgsDeterministic.and(targetDeterminsitic)

        return ExpressionResult(
            type = returnType,
            deterministic = deterministic,
            constValue = targetSummary?.returnSummary?.compileTimeValue ?: AbstractValue.Unknown,
            paramDeps = allParamDeps
        )
    }

    private val typeContext: Map<String, Type> get() = localVariables.mapValues { it.value.narrowedType }

    private fun buildVariableSummary(state: VariableState): VariableSummary {
        val effectivelyConstant: ReasonedBoolean =
            if (state.isConstant) {//Cant is constant be turned into a ReasonedBoolean itself and therefore be more percise
                True("Variable is not reassignable and its initializer is a compile-time constant")
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
