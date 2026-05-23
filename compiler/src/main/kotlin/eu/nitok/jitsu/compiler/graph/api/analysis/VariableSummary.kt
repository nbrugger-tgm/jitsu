package eu.nitok.jitsu.compiler.graph.api.analysis

import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.compiler.analysis.AbstractValueElement
import eu.nitok.jitsu.compiler.analysis.OwnershipState
import eu.nitok.jitsu.compiler.graph.api.Type

/**
 * Per-variable-declaration analysis summary.
 * Attached to [eu.nitok.jitsu.compiler.graph.elements.VariableDeclaration] and [eu.nitok.jitsu.compiler.graph.elements.FunctionElement.Parameter].
 */
interface VariableSummary {
    /** User-annotated type, null if type was inferred. */
    val declaredType: Type?

    /** Tightest type that analysis can prove for this variable. */
    val narrowedType: Type

    /** Whether the variable is never reassigned and its initializer is constant. */
    val effectivelyConstant: ReasonedBoolean

    /** Compile-time known value, if any. */
    val compileTimeValue: AbstractValue
    val ownershipState: OwnershipState
}