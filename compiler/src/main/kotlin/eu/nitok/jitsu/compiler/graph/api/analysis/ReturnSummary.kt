package eu.nitok.jitsu.compiler.graph.api.analysis

import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.compiler.graph.api.Type

interface ReturnSummary {
    /** All types that could be returned across all return paths. Includes Type.Null if nullable. */
    val possibleTypes: List<Type>

    /** Whether the return value is compile-time known. */
    val compileTimeValue: AbstractValue

    /** Which parameters influence the return value. */
    val dependsOnParameters: Set<String>
    val deterministic: ReasonedBoolean
}