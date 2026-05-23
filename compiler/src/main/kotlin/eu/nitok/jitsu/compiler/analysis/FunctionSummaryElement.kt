package eu.nitok.jitsu.compiler.analysis

import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.compiler.graph.api.analysis.FunctionSummary
import eu.nitok.jitsu.compiler.graph.api.analysis.ParameterMode
import eu.nitok.jitsu.compiler.graph.elements.FunctionElement
import eu.nitok.jitsu.compiler.merge
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Summary of a function's analyzed traits.
 * Produced by [CodeBlockAnalyzer] and managed by [AnalysisRepository].
 */
@Serializable
internal data class FunctionSummaryElement(
    /** Whether the function has no observable effects beyond its return value. */
    override val noSideEffects: ReasonedBoolean,

    /** BORROW/MOVE mode for each parameter by name. */
    override val parameterModes: Map<String, ParameterMode> = emptyMap(),

    /** Return value characteristics. Null for void functions. */
    override val returnSummary: ReturnSummaryElement? = null,

    /** Names of functions called by this function (for serialization/display). */
    @Transient override val callees: List<FunctionElement> = emptyList(), //TODO better solution needed at some point
    override val variableSummary: Map<String, VariableSummaryElement>
) : FunctionSummary {

    /** A function is pure if it is deterministic and has no side effects. */
    override val pure: Boolean get() = (returnSummary?.deterministic?.value?:true) && noSideEffects.value

    companion object {
        /**
         * Creates an optimistic initial summary for fixed-point iteration.
         * Must-properties start at their best value; may-properties start empty.
         */
        fun optimistic(parameterNames: List<String>): FunctionSummaryElement {
            val optimisticNoSideEffects = ReasonedBoolean.True("Optimistic seed: assumed no side effects until proven otherwise")
            return FunctionSummaryElement(
                noSideEffects = optimisticNoSideEffects,
                parameterModes = parameterNames.associateWith { ParameterMode.BORROW },
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
    fun refineWith(other: FunctionSummaryElement): FunctionSummaryElement {
        val refinedNoSideEffects = this.noSideEffects.and(other.noSideEffects)
        return FunctionSummaryElement(
            noSideEffects = refinedNoSideEffects,
            parameterModes = mergeParameterModes(this.parameterModes, other.parameterModes),
            returnSummary = mergeReturnSummaries(this.returnSummary, other.returnSummary),
            callees = (this.callees + other.callees).distinct(),
            variableSummary = mergeVariableSummaries(variableSummary, other.variableSummary)
        )
    }

    /**
     * Checks equality for fixed-point convergence (ignores reason text, only checks trait values).
     */
    fun structurallyEquals(other: FunctionSummary): Boolean {
        return noSideEffects == other.noSideEffects
                && parameterModes == other.parameterModes
                && returnSummary == other.returnSummary
                && callees.toSet() == other.callees.toSet()
    }
    private fun mergeVariableSummaries(
        a: Map<String, VariableSummaryElement>,
        b: Map<String, VariableSummaryElement>
    ): Map<String, VariableSummaryElement> {
        return merge(a,b) { a,b ->
            a.refineWith(b)
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

    private fun mergeReturnSummaries(a: ReturnSummaryElement?, b: ReturnSummaryElement?): ReturnSummaryElement? {
        if (a == null) return b
        if (b == null) return a
        return a.mergeWith(b)
    }
}