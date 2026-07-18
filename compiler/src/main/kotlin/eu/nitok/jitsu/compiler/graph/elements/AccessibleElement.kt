package eu.nitok.jitsu.compiler.graph.elements

import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.compiler.graph.SymbolID
import eu.nitok.jitsu.compiler.graph.api.Accessible
import eu.nitok.jitsu.compiler.graph.behaviour.ModuleAware

internal interface AccessibleElement<T: Accessible<T>> : Accessible<T>, ModuleAware {
    var symbolIndex: Int?
    var module: JitsuModule
    override val name: Located<String>?
    override val fullyQualifiedName get() = name?.let { "${module.fullyQualifiedName}.${it.value}" }

    fun symbolID(accessingModule: JitsuModule): SymbolID {
        val symid = symbolIndex
        return if (symid == null) error("Symbol index is not set for $this")
        else if (accessingModule !== module) SymbolID(module.fullyQualifiedName, symid)
        else SymbolID(null, symid)
    }

    fun getSymbol(module: JitsuModule): Int

    override fun setEnclosingModule(parent: JitsuModule) {
        symbolIndex = getSymbol(parent)
        module = parent
    }
}