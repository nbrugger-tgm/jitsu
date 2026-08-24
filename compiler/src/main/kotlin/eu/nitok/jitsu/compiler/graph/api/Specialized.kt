package eu.nitok.jitsu.compiler.graph.api

/**
 * Marks an object which was potentially created from a [Parameterized] object
 */
interface Specialized<out P : Parameterized> {
    /**
     * Null if this object was specialized by nature and not specialized from a parameterized object
     */
    val specializedFrom: P
}