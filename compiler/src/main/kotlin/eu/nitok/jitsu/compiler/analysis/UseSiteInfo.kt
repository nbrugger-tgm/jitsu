package eu.nitok.jitsu.compiler.analysis

import eu.nitok.jitsu.compiler.graph.elements.types.TypeElement
import eu.nitok.jitsu.compiler.graph.elements.types.Undefined
import kotlinx.serialization.Serializable

/**
 * Per-use-site analysis information.
 * Attached to each [eu.nitok.jitsu.compiler.graph.elements.VariableReference].
 */
@Serializable
internal data class UseSiteInfo(
    /** Type of the variable at this specific point of use. */
    val narrowedType: TypeElement = Undefined,
    /** Ownership state at this point of use. */
    val ownershipState: OwnershipState = OwnershipState.OWNS
)