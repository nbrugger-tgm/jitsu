package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.locating.Located
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

internal fun findAccessesFromSelf(elems: Iterable<Element>): List<Access<*>> {
    val lst = mutableListOf<Access<*>>()
    fun findAccesses(inst: Element) {
        if (inst is Access<*>) lst.add(inst)
        if (inst !is Accessor) inst.children.forEach(::findAccesses)
    }
    elems.forEach(::findAccesses)
    return lst
}

@Serializable
sealed interface Access<T : Accessible<T>>{
    val target: T?
    val reference: Located<String>

    sealed interface FunctionAccess : Access<Function>
    sealed interface VariableAccess : Access<Variable> {
        val accessKind : AccessKind
        enum class AccessKind {
            /**
             * The parameter does not leave the scope and the callee can still use/manage the variable afterward
             */
            BORROW,

            /**
             * The parameter needs to be managed by the function and the callee needs to either give controll to the function
             * or hand the function a copy.
             */
            MOVE
        }
    }
    sealed interface TypeAccess : Access<TypeDefinition> {
        fun resolve(messages: CompilerMessages): TypeDefinition
    }
}

@Serializable
abstract class AccessImpl<T: Accessible<T>>: Access<T>, ScopeAware, ModuleAware {
    var targetId: SymbolID? = null
    @Transient lateinit var scope: Scope
    @Transient override lateinit var module: JitsuModule
    @Transient final override var target: T? = null
        private set;
    @Transient protected abstract val restore: JitsuModule.(Int)->T?
    @Transient protected abstract val getSymbolId: JitsuModule.(T)-> SymbolID

    override fun setEnclosingScope(parent: Scope) {
        this.scope = parent
    }
    internal fun restore(messages: CompilerMessages) {
        val symbol = targetId ?: throw IllegalStateException("Cannot restore without symbol ID")
        if(target != null) throw IllegalStateException("Cannot restore twice")
        target = if(symbol.module == null) module.restore(symbol.index)
        else scope.restore(reference.map { symbol }, messages, restore)
        if(target != null) target!!.accessToSelf.add(this)
    }

    internal fun setResolvedTarget(target: T) {
        if(this.target != null) this.target?.accessToSelf?.remove(this)
        this.target = target
        target.accessToSelf.add(this)
        val symbol = if(target.module.name == this.module.name) SymbolID(null,this.module.getSymbolId(target).index)
        else target.module.getSymbolId(target)
        this.targetId = symbol
    }

    override fun setEnclosingModule(parent: JitsuModule) {
        this.module = parent
    }
}