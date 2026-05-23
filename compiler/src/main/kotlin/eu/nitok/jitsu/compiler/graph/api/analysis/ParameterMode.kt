package eu.nitok.jitsu.compiler.graph.api.analysis

import kotlinx.serialization.Serializable

@Serializable
enum class ParameterMode {
    /** The parameter does not leave the scope; callee can still use the variable afterward. */
    BORROW,
    /** The parameter is consumed by the function; callee must give ownership or copy. */
    MOVE;

    internal fun refineWith(other: ParameterMode): ParameterMode =
        if (this == MOVE || other == MOVE) MOVE else BORROW
}