package eu.nitok.jitsu.compiler.graph.api

import eu.nitok.jitsu.common.locating.Located

/**
 * An element with a name that maybe referenced by other [Access] elements by that name
 */
interface Accessible<out T : Accessible<T>> {
    val accessToSelf: List<Access<T>>
    val name: Located<String>?
    val fullyQualifiedName: String?
}