package eu.nitok.jitsu.compiler.graph.elements

import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.compiler.graph.SymbolID
import eu.nitok.jitsu.compiler.graph.api.Access
import eu.nitok.jitsu.compiler.graph.api.Accessible
import eu.nitok.jitsu.compiler.graph.behaviour.ModuleAware

/**
 * An element that can be targeted by an [AccessElement] and therefore needs a symbol to be (de)serializable.
 *
 * This is the type erased half of [AccessibleElement]. It exists so elements that are [Accessible] with different
 * type arguments (e.g. the two branches of [eu.nitok.jitsu.compiler.graph.api.TypeDefinition]) can share a supertype.
 */
internal interface SymbolElement : ModuleAware {
    var symbolIndex: Int?
    var module: JitsuModule

    /**
     * Type erased, mutable view of [Accessible.accessToSelf]. Only [AccessElement]s should write to this.
     */
    val mutableAccessToSelf: MutableList<Access<*>>

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

internal interface AccessibleElement<out T : Accessible<T>> : Accessible<T>, SymbolElement {
    /**
     * Mutable view of [Accessible.accessToSelf], written by [AccessElement]s upon resolution
     */
    override val accessToSelf: MutableList<Access<@UnsafeVariance T>>
    override val name: Located<String>?
    override val fullyQualifiedName get() = name?.let { "${module.fullyQualifiedName}.${it.value}" }

    @Suppress("UNCHECKED_CAST")
    override val mutableAccessToSelf: MutableList<Access<*>>
        get() = accessToSelf as MutableList<Access<*>>
}
