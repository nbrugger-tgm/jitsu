package eu.nitok.jitsu.compiler.analysis

import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.compiler.graph.Access
import eu.nitok.jitsu.compiler.graph.Function
import eu.nitok.jitsu.compiler.graph.Type
import eu.nitok.jitsu.compiler.graph.Variable
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.collections.mutableMapOf

/**
 * Summary of a function's analyzed traits.
 * Produced by [CodeBlockAnalyzer] and managed by [AnalysisRepository].
 */
@Serializable
data class FunctionSummary(
    /** Whether same inputs always produce the same output. */
    val deterministic: ReasonedBoolean,

    /** Whether the function has no observable effects beyond its return value. */
    val noSideEffects: ReasonedBoolean,

    /** BORROW/MOVE mode for each parameter by name. */
    val parameterModes: Map<String, ParameterMode> = emptyMap(),

    /** Which parameters influence the return value. */
    val parameterInfluence: Set<String> = emptySet(),

    /** Return value characteristics. Null for void functions. */
    val returnSummary: ReturnSummary? = null,

    /** Names of functions called by this function (for serialization/display). */
    val callees: List<Function> = emptyList(),
    val variableSummary: Map<Variable, VariableSummary>
) {
    /** A function is pure if it is deterministic and has no side effects. */
    val pure: Boolean get() = deterministic.value && noSideEffects.value

    companion object {
        /**
         * Creates an optimistic initial summary for fixed-point iteration.
         * Must-properties start at their best value; may-properties start empty.
         */
        fun optimistic(parameterNames: List<String>): FunctionSummary {
            val optimisticDeterministic = ReasonedBoolean.True("Optimistic seed: assumed deterministic until proven otherwise")
            val optimisticNoSideEffects = ReasonedBoolean.True("Optimistic seed: assumed no side effects until proven otherwise")
            return FunctionSummary(
                deterministic = optimisticDeterministic,
                noSideEffects = optimisticNoSideEffects,
                parameterModes = parameterNames.associateWith { ParameterMode.BORROW },
                parameterInfluence = emptySet(),
                returnSummary = null,
                callees = emptyList(),
                variableSummary = mapOf()
            )
        }
    }

    /**
     * Refines this summary by combining with [other] using lattice rules.
     * Must-properties weaken (&&), may-properties grow (union).
     */
    fun refineWith(other: FunctionSummary): FunctionSummary {
        val refinedDeterministic = this.deterministic.and(other.deterministic)
        val refinedNoSideEffects = this.noSideEffects.and(other.noSideEffects)
        return FunctionSummary(
            deterministic = refinedDeterministic,
            noSideEffects = refinedNoSideEffects,
            parameterModes = mergeParameterModes(this.parameterModes, other.parameterModes),
            parameterInfluence = this.parameterInfluence + other.parameterInfluence,
            returnSummary = mergeReturnSummaries(this.returnSummary, other.returnSummary),
            callees = (this.callees + other.callees).distinct(),
            variableSummary = mergeVariableSummaries(variableSummary, other.variableSummary)
        )
    }

    /**
     * Checks equality for fixed-point convergence (ignores reason text, only checks trait values).
     */
    fun structurallyEquals(other: FunctionSummary): Boolean {
        return deterministic == other.deterministic
                && noSideEffects == other.noSideEffects
                && parameterModes == other.parameterModes
                && parameterInfluence == other.parameterInfluence
                && returnSummary == other.returnSummary
                && callees.toSet() == other.callees.toSet()
    }
}

@Serializable
enum class ParameterMode {
    /** The parameter does not leave the scope; callee can still use the variable afterward. */
    BORROW,
    /** The parameter is consumed by the function; callee must give ownership or copy. */
    MOVE;

    fun refineWith(other: ParameterMode): ParameterMode =
        if (this == MOVE || other == MOVE) MOVE else BORROW
}

/**
 * Characteristics of a function's return value.
 */
@Serializable
data class ReturnSummary(
    /** All types that could be returned across all return paths. Includes Type.Null if nullable. */
    val possibleTypes: List<Type> = emptyList(),
    /** Whether the return value is compile-time known. */
    val compileTimeValue: AbstractValue = AbstractValue.NoValue,
    /** Which parameters influence the return value. */
    val dependsOnParameters: Set<String> = emptySet()
) {
    fun mergeWith(other: ReturnSummary): ReturnSummary {
        return ReturnSummary(
            possibleTypes = (this.possibleTypes + other.possibleTypes).distinct(),
            compileTimeValue = this.compileTimeValue.join(other.compileTimeValue),
            dependsOnParameters = this.dependsOnParameters + other.dependsOnParameters
        )
    }
}

/**
 * Per-variable-declaration analysis summary.
 * Attached to [eu.nitok.jitsu.compiler.graph.VariableDeclaration] and [Function.Parameter].
 */
@Serializable
data class VariableSummary(
    /** User-annotated type, null if type was inferred. */
    val declaredType: Type? = null,
    /** Tightest type that analysis can prove for this variable. */
    val narrowedType: Type = Type.Undefined,
    /** Whether the variable is never reassigned and its initializer is constant. */
    val effectivelyConstant: ReasonedBoolean = ReasonedBoolean.False("default"),
    /** Compile-time known value, if any. */
    val compileTimeValue: AbstractValue = AbstractValue.Unknown,
    val ownershipState: OwnershipState
) {
    fun refineWith(modeB: VariableSummary): VariableSummary {
        return VariableSummary(
            declaredType?: modeB.declaredType,
            narrowedType,
            effectivelyConstant.and(modeB.effectivelyConstant),
            compileTimeValue.join(modeB.compileTimeValue),
            ownershipState.join(modeB.ownershipState)
        )
    }
}

/**
 * Per-use-site analysis information.
 * Attached to each [eu.nitok.jitsu.compiler.graph.Expression.VariableReference].
 */
@Serializable
data class UseSiteInfo(
    /** Type of the variable at this specific point of use. */
    val narrowedType: Type = Type.Undefined,
    /** Ownership state at this point of use. */
    val ownershipState: OwnershipState = OwnershipState.OWNS
)

@Serializable
enum class OwnershipState {
    /** This scope owns the data and is responsible for cleanup. */
    OWNS,
    /** This scope only borrows (reads) the data. */
    BORROWS,
    /** The variable has been moved and is no longer available. */
    MOVED;

    fun join(other: OwnershipState ): OwnershipState {
        return when {
            this == MOVED || other == MOVED -> MOVED
            this == OWNS || other == OWNS -> OWNS
            else -> BORROWS
        }
    }
}

/**
 * Abstract representation of a compile-time value.
 * Forms a three-element lattice: NoValue ⊑ Const(c) ⊑ Unknown
 */
@Serializable
sealed interface AbstractValue {
    /** No value observed (unreachable code path, or no return). */
    @Serializable
    data object NoValue : AbstractValue {
        override fun toString(): String = "NoValue"
    }

    /** A known compile-time constant value. Stored as string for serialization. */
    @Serializable
    data class Const(val value: String, val valueType: Type) : AbstractValue {
        override fun toString(): String = "Const($value: $valueType)"
    }

    /** Value exists but is not compile-time determinable. */
    @Serializable
    data object Unknown : AbstractValue {
        override fun toString(): String = "Unknown"
    }

    /**
     * Lattice join: combines two abstract values.
     * - NoValue is the bottom element (absorbs into the other).
     * - Same Const stays Const.
     * - Different Consts or any Unknown produces Unknown.
     */
    fun join(other: AbstractValue): AbstractValue = when {
        this is NoValue -> other
        other is NoValue -> this
        this is Const && other is Const && this == other -> this
        else -> Unknown
    }
}

// --- Internal helpers ---

private fun mergeVariableSummaries(
    a: Map<Variable, VariableSummary>,
    b: Map<Variable, VariableSummary>
): Map<Variable, VariableSummary> {
    val allKeys = a.keys + b.keys
    return allKeys.associateWith { key ->
        val modeA = a[key] ?: null
        val modeB = b[key] ?: null
        if(modeA == null) modeB!!
        else if(modeB == null) modeA!!
        else modeA.refineWith(modeB)
    }
}
private fun mergeParameterModes(
    a: Map<String, ParameterMode>,
    b: Map<String, ParameterMode>
): Map<String, ParameterMode> {
    val allKeys = a.keys + b.keys
    return allKeys.associateWith { key ->
        val modeA = a[key] ?: ParameterMode.BORROW
        val modeB = b[key] ?: ParameterMode.BORROW
        modeA.refineWith(modeB)
    }
}

private fun mergeReturnSummaries(a: ReturnSummary?, b: ReturnSummary?): ReturnSummary? {
    if (a == null) return b
    if (b == null) return a
    return a.mergeWith(b)
}
