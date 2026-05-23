package eu.nitok.jitsu.compiler.graph.api

import kotlinx.serialization.Transient

/**
 * An element that itseld if not an [Access] but can contain [accesses][Access]
 */
interface Accessor {
    @Transient val accessFromSelf: List<Access<*>>
}