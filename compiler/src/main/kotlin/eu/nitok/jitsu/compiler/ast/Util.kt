package eu.nitok.jitsu.compiler.ast

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
sealed interface N<out T> {
    fun <M> map(function: (T) -> M): N<M> {
        return when (this) {
            is Node -> Node(function(value))
            is Error -> Error(this.node, message)
        }
    }

    fun location(valueMapper: (T)-> Location): Location {
        return when (this) {
            is Node -> valueMapper(value)
            is Error -> node
        }
    }

    val warnings : MutableList<Error<Any>>;
    fun warning(error: Error<Any>?): N<T> {
        error?.let { warnings.add(it) }
        return this
    }
    @Serializable
    data class Node<T>(val value: T) : N<T> {
        override val warnings = mutableListOf<Error<@Contextual Any>>()
    }
    @Serializable
    data class Error<T>(val node: Location, val message: String, val explicitErrorLocation: Location? = null) : N<T> {
        override val warnings = mutableListOf<Error<@Contextual Any>>()
        fun <M>casted(): Error<M> {
            return this as Error<M>
        }
    }
}


typealias Location = @Serializable(with = LocationSerializer::class) com.niton.parser.token.Location
typealias Located<T> = Pair<T, Location>