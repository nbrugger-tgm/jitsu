package eu.nitok.jitsu.compiler.analysis

import kotlinx.serialization.Serializable

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

