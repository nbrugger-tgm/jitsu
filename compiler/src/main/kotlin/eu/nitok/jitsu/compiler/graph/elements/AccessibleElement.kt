package eu.nitok.jitsu.compiler.graph.elements

import eu.nitok.jitsu.compiler.graph.SymbolID
import eu.nitok.jitsu.compiler.graph.behaviour.ModuleAware

internal interface AccessibleElement : ModuleAware {
    var symbolIndex: Int
    var module: JitsuModule
    fun symbolID(accessingModule: JitsuModule) =
        if (accessingModule !== module) SymbolID(module.fullyQualifiedName, symbolIndex)
        else SymbolID(null, symbolIndex)

    fun getSymbol(module: JitsuModule): Int

    override fun setEnclosingModule(parent: JitsuModule) {
        symbolIndex = getSymbol(parent)
        module = parent
    }
}