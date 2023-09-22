package eu.nitok.jitsu.compiler.ast

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A node that can be an error
 */
@Serializable
sealed interface N<out T> {
    fun <M> map(function: (T) -> M): N<M> {
        return when (this) {
            is Node -> Node(function(value))
            is Error -> Error(this.node, message)
        }
    }

    fun location(valueMapper: (T) -> Location): Location {
        return when (this) {
            is Node -> valueMapper(value)
            is Error -> node
        }
    }

    val warnings: MutableList<Error<Any>>;
    fun warning(error: Error<Any>?): N<T> {
        error?.let { warnings.add(it) }
        return this
    }

    fun orNull(): T? {
        return when (this) {
            is Error -> null
            is Node -> value
        }
    }

    class Node<T>(value: T) : W<T>(value), N<T> {
        override fun toString(): String {
            return value.toString()
        }
        override fun warning(error: Error<Any>?): Node<T> {
            super<W>.warning(error)
            return this
        }
    }

    @Serializable
    data class Error<T>(val node: Location, val message: String, val explicitErrorLocation: Location? = null) : N<T> {
        override val warnings = mutableListOf<Error<@Contextual Any>>()
        fun <M> casted(): Error<M> {
            return this as Error<M>
        }

        override fun toString(): String {
            return "[$node] $message"
        }
    }
}

@Serializable
open class W<out T>(val value: T) {
    val warnings = mutableListOf<N.Error<@Contextual Any>>()
    open fun warning(error: N.Error<Any>?): W<T> {
        error?.let { warnings.add(it) }
        return this
    }

    fun toNode() : N.Node<T> {
        if(this is N.Node) return this;
        else N.Node(value).let {
            it.warnings.addAll(warnings)
            it
        }
    }
}

public fun <T> orElse(node: N<T>?, default: T): T {
    return when (node) {
        is N.Node -> node.value
        else -> default
    }
}

val N<Located<Any>>.location
    get() : Location {
        return this.location { it.location }
    }
typealias Location = @Serializable(with = LocationSerializer::class) com.niton.parser.token.Location
typealias Located<T> = Pair<T, Location>

val Located<*>.location get() = second;
val <T> Located<T>.value: T get() = first;