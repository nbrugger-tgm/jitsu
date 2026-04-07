package eu.nitok.jitsu.common

import kotlinx.serialization.Serializable

@Serializable
data class Located<T>(val value: T, val location: Range) {
    inline fun <N> map(fn: (T)->N): Located<N> {
        return Located(fn(value), location)
    }
}

fun <T> T.locatedAt(location: Range): Located<T> {
    return Located(this, location)
}