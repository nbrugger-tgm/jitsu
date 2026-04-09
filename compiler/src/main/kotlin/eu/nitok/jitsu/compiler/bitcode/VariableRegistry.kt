package eu.nitok.jitsu.compiler.bitcode

import eu.nitok.jitsu.compiler.analysis.OwnershipState
import eu.nitok.jitsu.compiler.graph.Function
import eu.nitok.jitsu.compiler.graph.Type
import eu.nitok.jitsu.compiler.graph.VariableDeclaration

public class VariableRegistry(val function: Function) {
    data class Entry(val requiresFree: Boolean, val name: String, val type: Type)
    private val entries = mutableMapOf<VariableDeclaration, Entry>()

    fun getEntry(variable: VariableDeclaration): Entry {
        if (variable in entries) {
            return entries[variable]!!
        }
        val entry = Entry(
            requiresFree = function.summary?.variableSummary?.get(variable)?.ownershipState == OwnershipState.OWNS,
            name = variable.name.value,
            type = variable.type
        )
        entries[variable] = entry
        return entry
    }

    val variablesToFree: Set<Entry> = entries.values.filter { it.requiresFree }.toSet()
}