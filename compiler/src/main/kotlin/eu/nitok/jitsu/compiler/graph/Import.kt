package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.locating.HasLocation
import eu.nitok.jitsu.common.locating.Locatable
import eu.nitok.jitsu.common.locating.Located
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Import(val name: Located<String>): Element, HasLocation {
    override val children: List<Element> get() = listOf()

    @Transient var target: JitsuModule? = null
    fun resolve(messages: CompilerMessages, modules: Map<String, JitsuModule>): JitsuModule? {
        target?.let { return it }
        val x = modules[name.value]
        if(x == null) messages.error("Module ${name.value} not found", name)
        target = x
        return target
    }

    override val location: Locatable get() = name.location
}
