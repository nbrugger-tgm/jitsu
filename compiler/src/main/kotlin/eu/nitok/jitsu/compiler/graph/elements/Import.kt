package eu.nitok.jitsu.compiler.graph.elements

import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.locating.Locatable
import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.compiler.graph.api.Element
import eu.nitok.jitsu.compiler.graph.api.Import
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal data class Import(override val name: Located<String>): Import {
    override val children: List<Element> get() = listOf()

    @Transient
    override var target: JitsuModule? = null

    fun resolve(messages: CompilerMessages, modules: Map<String, JitsuModule>): JitsuModule? {
        target?.let { return it }
        val x = modules[name.value]
        if(x == null) messages.error("Module ${name.value} not found", name)
        target = x
        return target
    }

    override val location: Locatable get() = name.location
}