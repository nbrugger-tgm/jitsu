package eu.nitok.jitsu.compiler.graph.api

/**
 * Marker interface for an element that may have [Attribute]s attached to it
 */
sealed interface HasAttributes : Element {
    val attributes: List<Attribute>
}