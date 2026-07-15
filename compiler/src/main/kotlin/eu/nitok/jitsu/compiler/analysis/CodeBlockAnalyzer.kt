package eu.nitok.jitsu.compiler.analysis

import eu.nitok.jitsu.common.CompilerMessage
import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.common.ReasonedBoolean.False
import eu.nitok.jitsu.common.ReasonedBoolean.True
import eu.nitok.jitsu.compiler.graph.api.*
import eu.nitok.jitsu.compiler.graph.api.analysis.ParameterMode
import eu.nitok.jitsu.compiler.graph.elements.FunctionElement
import eu.nitok.jitsu.compiler.graph.elements.VariableDeclaration
import eu.nitok.jitsu.compiler.graph.elements.ArrayLiteral
import eu.nitok.jitsu.compiler.graph.elements.ConstantElement
import eu.nitok.jitsu.compiler.graph.elements.ExpressionElement
import eu.nitok.jitsu.compiler.graph.elements.VariableReference
import eu.nitok.jitsu.compiler.graph.elements.FunctionCall
import eu.nitok.jitsu.compiler.graph.elements.InstructionElement
import eu.nitok.jitsu.compiler.graph.elements.Return
import eu.nitok.jitsu.compiler.graph.elements.UndefinedExpression
import eu.nitok.jitsu.compiler.graph.elements.VariableElement
import eu.nitok.jitsu.compiler.graph.elements.types.TypeElement
import eu.nitok.jitsu.compiler.graph.elements.types.Undefined

internal class CodeBlockAnalyzer(
    private val function: FunctionElement,
    private val calleeOracle: (FunctionElement) -> FunctionSummaryElement?,
    private val messages: CompilerMessages
) {
    data class AnalysisResult(
        val functionSummary: FunctionSummaryElement,
        val useSiteInfos: Map<VariableReference, UseSiteInfo>
    )

    private data class VariableState(
        val declaredType: TypeElement?,
        var narrowedType: TypeElement,
        var ownershipState: OwnershipState,
        var isConstant: Boolean,
        var compileTimeValue: AbstractValueElement,
        var paramDeps: Set<String>,
        val variable: VariableElement,
        val deterministic: ReasonedBoolean
    )

    private data class ExpressionResult(
        /**
         * The type of the expression considering the expected type.
         * Example
         * `var x: i32[] = [1, 2]`
         * `[1, 2]` is of type `i8[]` but since it is assigned to a `i32[]` type would be `i32[]`
         */
        val type: TypeElement,
        /**
         * The actual type of the expression at this point in time
         * Example
         * `var x: i32[] = [1]`
         * `[1]` is of type `i8[]` even tho assigned to a `i32[]` the narrowed type is still i8[]
         */
        val narrowedType: TypeElement,
        val deterministic: ReasonedBoolean,
        val constValue: AbstractValueElement,
        val paramDeps: Set<String>
    )

    private val localVariables: MutableMap<String, VariableState> = mutableMapOf()
    private val useSiteInfos: MutableMap<VariableReference, UseSiteInfo> = mutableMapOf()
    private val returnPaths: MutableList<ReturnPathInfo> = mutableListOf()
    private val callees: MutableList<FunctionElement> = mutableListOf()
    private var hasSideEffects: ReasonedBoolean = True("No side effects observed yet")

    private data class ReturnPathInfo(
        val type: TypeElement,
        val compileTimeValue: AbstractValueElement,
        val paramDeps: Set<String>,
        val deterministic: ReasonedBoolean
    )

    fun analyze(): AnalysisResult {
        for (param in function.parameters) {
            localVariables[param.name.value] = VariableState(
                declaredType = param.declaredTypeElement,
                narrowedType = param.declaredTypeElement,
                ownershipState = OwnershipState.BORROWS,
                isConstant = false,
                compileTimeValue = AbstractValueElement.Unknown,
                paramDeps = setOf(param.name.value),
                variable = param,
                deterministic = True("Parameters are deterministic")
            )
        }
        if (function.body is FunctionElement.BodyElement.Implementation) {
            for (instruction in function.body.codeBlock.instructionElements) {
                processInstruction(instruction)
            }
        }

        val parameterModes: Map<String, ParameterMode> = function.parameters.associate { param: FunctionElement.Parameter ->
            val hasMoveAccess = (param.accessToSelf as List<*>)
                .filterIsInstance<Access.VariableAccess>()
                .any { access -> access.accessKind == Access.VariableAccess.AccessKind.MOVE }
            param.name.value to if (hasMoveAccess) ParameterMode.MOVE else ParameterMode.BORROW
        }

        val fnParams = function.parameters

        val returnSummary: ReturnSummaryElement? = if (returnPaths.isEmpty()) {
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
                ReturnSummaryElement(
                    possibleTypeElements = returnPaths.map { rp -> rp.type }.distinct(),
                    compileTimeValueElement = merged.compileTimeValue,
                    dependsOnParameters = merged.paramDeps
                        .filter { dep -> fnParams.any { p -> p.name.value == dep } }//Why is this needed? what else besides parameters is in this stream
                        .toSet(),
                    deterministic = merged.deterministic
                )
            }
        }

        val variableSummaries = mutableMapOf<String, VariableSummaryElement>()

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
        val functionSummary = FunctionSummaryElement(
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

    private fun processInstruction(instruction: InstructionElement) {
        when (instruction) {
            is VariableDeclaration -> processVariableDeclaration(instruction)
            is FunctionCall -> processFunctionCall(instruction)
            is Return -> processReturn(instruction)
            is FunctionElement -> TODO("nested functions not implemented yet")
        }
    }

    private fun processVariableDeclaration(decl: VariableDeclaration) {
        val expression: ExpressionResult? = decl.initialValueElement?.let { analyzeExpression(it, decl.declaredTypeElement?.rawTypeElement) }
        decl.implicitTypeElement = expression?.type
        val narrowedType: TypeElement? = when {
            expression != null -> expression.narrowedType
            decl.declaredType != null -> decl.declaredTypeElement
            else -> null
        }
        if (expression != null && decl.declaredType != null) {
            val assignedType = expression.type
            val initialTypeValid = decl.declaredType.acceptsInstanceOf(assignedType.asType)
            if (!initialTypeValid.value) {
                messages.error(initialTypeValid, decl.initialValueElement.location)
            }
        }

        val isConstant = !decl.reassignable && expression?.constValue is AbstractValueElement.Const

        val compileTimeValue: AbstractValueElement = expression?.constValue ?: AbstractValueElement.NoValue

        val paramDeps: Set<String> = expression?.paramDeps ?: emptySet()

        localVariables[decl.name.value] = VariableState(
            declaredType = decl.declaredTypeElement ?: narrowedType,
            narrowedType = narrowedType ?: decl.declaredTypeElement ?: Undefined,
            ownershipState = OwnershipState.OWNS,
            isConstant = isConstant,
            compileTimeValue = compileTimeValue,
            paramDeps = paramDeps,
            variable = decl,
            deterministic = expression?.deterministic ?: True("Variable not assigned yet")
        )
    }

    private fun processReturn(ret: Return) {
        val value = ret.valueExpression

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

        val exprResult = analyzeExpression(value, function.returnTypeElement?.value)

        if (function.returnType != null) {
            val typeMatches = function.returnType.value.acceptsInstanceOf(exprResult.type.asType)
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

    private fun analyzeExpression(expr: ExpressionElement, typeHint: TypeElement?): ExpressionResult {
        return when (expr) {
            is ConstantElement<*> -> analyzeConstant(expr, typeHint)
            is VariableReference -> analyzeVariableReference(expr, typeHint)
            is FunctionCall -> analyzeFunctionCallExpression(expr, typeHint)
            is UndefinedExpression -> ExpressionResult(
                type = typeHint?: Undefined,
                narrowedType = Undefined,
                deterministic = False("Undefined expression"),
                constValue = AbstractValueElement.Unknown,
                paramDeps = emptySet()
            )

            is ArrayLiteral -> analyzeArrayLiteral(expr, typeHint)
        }
    }

    private fun analyzeArrayLiteral(expr: ArrayLiteral, typeHint: TypeElement?): ExpressionResult {
        val arrayType = expr.calculateType(typeContext, messages, typeHint)
        val elementType = if (arrayType.elementType !is Type.Undefined) arrayType.elementTypeElement else null
        val elements = expr.elementExpressions.asSequence().map { analyzeExpression(it, elementType) }.toList()
        return ExpressionResult(
            type = arrayType,
            narrowedType = arrayType,
            deterministic = elements.map { it.deterministic }.fold(
                True("Empty array is deterministic") as ReasonedBoolean
            ) { acc, d -> acc.and(d) },
            constValue = AbstractValueElement.Unknown,
            paramDeps = elements.flatMap { it.paramDeps }.toSet()
        )
    }

    private fun analyzeConstant(constant: ConstantElement<*>, typeHint: TypeElement?): ExpressionResult {
        val type = constant.calculateType(typeContext, messages, typeHint)?: Undefined
        val narrowType = constant.calculateType(typeContext, messages)?: Undefined
        return ExpressionResult(
            type = type,
            narrowedType = narrowType,
            deterministic = True("Constant values are deterministic"),
            constValue = AbstractValueElement.Const(constant.literal, type),
            paramDeps = emptySet()
        )
    }

    private fun analyzeVariableReference(ref: VariableReference, typeHint: TypeElement?): ExpressionResult {
        val varName = ref.reference.value
        val varState = localVariables[varName]
        if (varState == null) {
            //Todo: global variable access
            messages.error("Variable '$varName' not found", ref.location)
            useSiteInfos[ref] = UseSiteInfo(
                narrowedType = Undefined,
                ownershipState = OwnershipState.BORROWS
            )
            return ExpressionResult(
                type = typeHint?: Undefined,
                narrowedType = Undefined,
                deterministic = True("Variable '$varName' not found"),
                constValue = AbstractValueElement.NoValue,
                paramDeps = emptySet()
            )
        }

        ref.setResolvedTarget(varState.variable)
        ref.accessKind = Access.VariableAccess.AccessKind.BORROW

        val type = ref.calculateType(typeContext, messages, typeHint) ?: Undefined

        useSiteInfos[ref] = UseSiteInfo(
            narrowedType = varState.narrowedType,
            ownershipState = varState.ownershipState
        )

        return ExpressionResult(
            type = type,
            narrowedType = varState.narrowedType,
            deterministic = varState.deterministic,
            constValue = varState.compileTimeValue,
            paramDeps = varState.paramDeps
        )
    }

    private fun processFunctionCall(call: FunctionCall): FunctionElement? {
        val target = call.targetElement ?: call.resolveTarget(typeContext, messages)
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

    private fun analyzeFunctionCallExpression(call: FunctionCall, typeHint: TypeElement?): ExpressionResult {
        val target = processFunctionCall(call)
        val targetSummary = target?.let { calleeOracle(it) }
        val argumentExpressions = call.callParameterElements.mapIndexed { index, expression ->
            if (target != null) analyzeExpression(expression, target.parameters.getOrNull(index)?.declaredTypeElement)
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
        val returnType = if(target != null) call.calculateType(typeContext, messages, typeHint) ?: run {
            messages.error("Expected value but ${call.reference.value} is a void function", call.location)
            Undefined
        } else Undefined
        val targetDeterminsitic =
            if (targetSummary?.returnSummary?.deterministic == null) True("Unresolved functions are assumed to be deterministic")
            else if (targetSummary.returnSummary.deterministic.value) True("$target is deterministic")
            else False("$target is not deterministic", targetSummary.returnSummary.deterministic)
        val deterministic = areOutputInfluencingArgsDeterministic.and(targetDeterminsitic)

        return ExpressionResult(
            type = returnType,
            narrowedType = returnType,
            deterministic = deterministic,
            constValue = targetSummary?.returnSummary?.compileTimeValueElement ?: AbstractValueElement.Unknown,
            paramDeps = allParamDeps
        )
    }

    private val typeContext: Map<String, TypeElement> get() = localVariables.mapValues { it.value.narrowedType }

    private fun buildVariableSummary(state: VariableState): VariableSummaryElement {
        val effectivelyConstant: ReasonedBoolean =
            if (state.isConstant) {//Cant is constant be turned into a ReasonedBoolean itself and therefore be more percise
                True("Variable is not reassignable and its initializer is a compile-time constant")
            } else {
                ReasonedBoolean.False("Variable is either reassignable or its initializer is not a compile-time constant")
            }
        return VariableSummaryElement(
            declaredTypeElement = state.declaredType,
            narrowedTypeElement = state.narrowedType,
            effectivelyConstant = effectivelyConstant,
            compileTimeValueElement = state.compileTimeValue,
            ownershipState = state.ownershipState
        )
    }
}
