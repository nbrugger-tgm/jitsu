package eu.nitok.jitsu.compiler.graph.elements

import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.compiler.graph.SymbolID
import eu.nitok.jitsu.compiler.graph.api.*
import eu.nitok.jitsu.compiler.graph.api.Function
import eu.nitok.jitsu.compiler.graph.behaviour.ModuleAware
import eu.nitok.jitsu.compiler.graph.behaviour.Resolvable
import eu.nitok.jitsu.compiler.graph.behaviour.Restorable
import eu.nitok.jitsu.compiler.graph.behaviour.ScopeAware
import eu.nitok.jitsu.compiler.graph.elements.types.TypeDefinitionElement
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

private typealias GetSymbolIdFn<T> = JitsuModule.(T) -> SymbolID

/**
 * This whole structure is needed due to https://youtrack.jetbrains.com/issue/KT-83
 */
internal interface AccessElement<T : Accessible<T>, TE : AccessibleElement<T>> : Access<T>, ScopeAware, ModuleAware, Restorable {
    fun setResolvedTarget(target: TE)
    val targetElement: TE?

    @Serializable
    abstract class AccessImpl<TE : AccessibleElement<T>, T : Accessible<T>> : AccessElement<T, TE> {
        var targetId: SymbolID? = null

        @Transient
        lateinit var scope: Scope

        @Transient
        lateinit var module: JitsuModule

        @Transient
        final override var targetElement: TE? = null
            private set

        @Transient
        final override var target: T? = null
            private set

        protected abstract val restore: JitsuModule.(Int) -> TE?

        override fun setEnclosingScope(parent: Scope) {
            this.scope = parent
        }

        abstract fun getApiValue(element: TE): T

        override fun restore(messages: CompilerMessages) {
            val symbol = targetId
            if (symbol == null) {
                messages.error(
                    "This element was not resolved prior to serialisation, erroneous programms shouldn't be serialized",
                    this.reference
                )
                return
            }
            if (target != null) throw IllegalStateException("Cannot restore twice")
            setResolvedTarget(
                (if (symbol.module == null) module.restore(symbol.index)
                else scope.restore(reference.map { symbol }, messages, restore))?:error("Dangling symbolid $symbol")
            )
            if (target != null) target!!.accessToSelf.add(this)
        }

        fun setTargetElement(target: TE?) {
            if (target == null) return
            this.targetElement = target
            this.target = getApiValue(target)
        }

        override fun setResolvedTarget(target: TE) {
            if (this.target != null) this.target?.accessToSelf?.remove(this)
            setTargetElement(target)
            this.target?.accessToSelf?.add(this)
            val symbol = target.symbolID(module)
            this.targetId = symbol
        }

        override fun setEnclosingModule(parent: JitsuModule) {
            this.module = parent
        }
    }

    interface FunctionAccess : AccessElement<Function, FunctionElement>, Access.FunctionAccess

    @Serializable
    class FunctionAccessElement(override val reference: Located<String>) :
        AccessImpl<FunctionElement, Function>(),
        FunctionAccess {
        @Transient
        override val restore = JitsuModule::getFunction

        override fun getApiValue(element: FunctionElement): Function = element
    }


    interface TypeAccess : AccessElement<TypeDefinition, TypeDefinitionElement>, Access.TypeAccess, Resolvable
    @Serializable
    class TypeAccessElement(override val reference: Located<String>) :
        AccessImpl<TypeDefinitionElement, TypeDefinition>(), TypeAccess {
        @Transient
        override val restore = JitsuModule::getType

        override fun getApiValue(element: TypeDefinitionElement): TypeDefinition = element.asTypeDefinition

        override fun resolve(messages: CompilerMessages) {
            target?.let { return }
            val resolveType = scope.resolveType(reference, messages)
            if (resolveType != null) {
                setResolvedTarget(resolveType)
            }
        }
    }

    interface VariableAccess : AccessElement<Variable, VariableElement>

    @Serializable
    class VariableAccessElement(
        override val reference: Located<String>
    ) : AccessImpl<VariableElement, Variable>(), VariableAccess {
        @Transient
        override val restore = JitsuModule::getVariable

        override fun getApiValue(element: VariableElement): Variable = element.asVariable
    }

    interface AttributeAccess : AccessElement<AttributeDefinition, AttributeDefinitionElement>, Resolvable
    @Serializable
    open class AttributeAccessElement(
        override val reference: Located<String>
    ) : AccessImpl<AttributeDefinitionElement, AttributeDefinition>(), AttributeAccess {
        @Transient
        override val restore: JitsuModule.(Int) -> AttributeDefinitionElement? = JitsuModule::getAttribute

        override fun getApiValue(element: AttributeDefinitionElement) = element
        override fun resolve(messages: CompilerMessages) {
            target?.let { return }
            val resolveAttribute = scope.resolveAttribute(reference, messages)
            if (resolveAttribute != null) {
                setResolvedTarget(resolveAttribute)
            }
        }

    }
}