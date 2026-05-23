package eu.nitok.jitsu.compiler.analysis

import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.compiler.graph.api.Type
import eu.nitok.jitsu.compiler.graph.api.analysis.AbstractValue
import eu.nitok.jitsu.compiler.graph.api.analysis.ReturnSummary
import eu.nitok.jitsu.compiler.graph.elements.types.TypeElement
import kotlinx.serialization.Serializable

/**
 * Characteristics of a function's return value.
 */
@Serializable
internal data class ReturnSummaryElement(
    /** All types that could be returned across all return paths. Includes Type.Null if nullable. */
    val possibleTypeElements: List<TypeElement> = emptyList(),
    /** Whether the return value is compile-time known. */
    val compileTimeValueElement: AbstractValueElement = AbstractValueElement.NoValue,
    /** Which parameters influence the return value. */
    override val dependsOnParameters: Set<String> = emptySet(),
    override val deterministic: ReasonedBoolean
) : ReturnSummary {
    override val possibleTypes: List<Type> by lazy { possibleTypeElements.map { it.asType } }
    override val compileTimeValue: AbstractValue get() = compileTimeValueElement.asAbstractValue
    fun mergeWith(other: ReturnSummaryElement): ReturnSummaryElement {
        return ReturnSummaryElement(
            possibleTypeElements = (this.possibleTypeElements + other.possibleTypeElements).distinct(),
            compileTimeValueElement = this.compileTimeValueElement.join(other.compileTimeValueElement),
            dependsOnParameters = this.dependsOnParameters + other.dependsOnParameters,
            deterministic = this.deterministic.and(other.deterministic),
        )
    }
}