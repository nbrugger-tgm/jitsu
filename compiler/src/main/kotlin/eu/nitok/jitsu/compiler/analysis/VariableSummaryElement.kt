package eu.nitok.jitsu.compiler.analysis

import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.compiler.graph.api.Type
import eu.nitok.jitsu.compiler.graph.api.analysis.AbstractValue
import eu.nitok.jitsu.compiler.graph.api.analysis.VariableSummary
import eu.nitok.jitsu.compiler.graph.elements.types.TypeElement
import eu.nitok.jitsu.compiler.graph.elements.types.Undefined
import kotlinx.serialization.Serializable

@Serializable
internal data class VariableSummaryElement(
    /** User-annotated type, null if type was inferred. */
    val declaredTypeElement: TypeElement? = null,
    /** Tightest type that analysis can prove for this variable. */
    val narrowedTypeElement: TypeElement = Undefined,
    /** Whether the variable is never reassigned and its initializer is constant. */
    override val effectivelyConstant: ReasonedBoolean = ReasonedBoolean.False("default"),
    /** Compile-time known value, if any. */
    val compileTimeValueElement: AbstractValueElement = AbstractValueElement.Unknown,
    override val ownershipState: OwnershipState
) : VariableSummary {
    override val declaredType: Type get() = narrowedTypeElement.asType
    override val narrowedType: Type get() = narrowedTypeElement.asType
    override val compileTimeValue: AbstractValue get() = compileTimeValueElement.asAbstractValue

    fun refineWith(modeB: VariableSummaryElement): VariableSummaryElement {
        return VariableSummaryElement(
            declaredTypeElement?: modeB.declaredTypeElement,
            narrowedTypeElement,
            effectivelyConstant.and(modeB.effectivelyConstant),
            compileTimeValueElement.join(modeB.compileTimeValueElement),
            ownershipState.join(modeB.ownershipState)
        )
    }
}