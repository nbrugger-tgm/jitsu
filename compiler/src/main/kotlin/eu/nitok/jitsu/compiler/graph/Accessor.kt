package eu.nitok.jitsu.compiler.graph

import kotlinx.serialization.Transient

interface Accessor {
    @Transient val accessFromSelf: List<Access<*>>
}