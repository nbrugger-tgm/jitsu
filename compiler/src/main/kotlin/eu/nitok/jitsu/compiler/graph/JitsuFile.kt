package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.model.sequence
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class JitsuFile(
    override val scope: Scope,
) : Accessor {
    @Transient
    override val accessFromSelf: List<Access<*>> = scope.getFunctions().flatMap {
        it.sequence().filterIsInstance<Access<*>>().onEach { it.accessor = this }
    }
}