package eu.nitok.jitsu.compiler.graph.api

import eu.nitok.jitsu.common.locating.Located

/**
 * An element with a name that maybe referenced by other [Access] elements by that name
 */
interface Accessible<T : Accessible<T>> {
    val accessToSelf: MutableList<Access<T>>
    val name: Located<String>?
}