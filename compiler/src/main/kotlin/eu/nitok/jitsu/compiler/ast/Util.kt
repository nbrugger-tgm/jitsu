package eu.nitok.jitsu.compiler.ast

import com.niton.parser.token.Location

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

    data class Node<T>(val value: T) : N<T>
    data class Error<T>(val node: Location, val message: String, val explicitErrorLocation: Location? = null) : N<T> {}
}


typealias Located<T> = Pair<T, Location>