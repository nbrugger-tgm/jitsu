package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.common.locating.Located
import kotlinx.serialization.Transient

sealed interface Accessible<T : Accessible<T>>: ModuleAware {
    @Transient val accessToSelf: MutableList<Access<T>>
    val name: Located<String>?
}