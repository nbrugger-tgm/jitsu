package eu.nitok.jitsu.common.locating

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.lang.reflect.ParameterizedType

@Serializable
data class Located<T>(val value: T, override val location: Location) : HasLocation {
    inline fun <N> map(fn: (T)->N): Located<N> {
        return Located(fn(value), location)
    }
}

fun <T> T.locatedAt(location: Location): Located<T> {
    return Located(this, location)
}