package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.common.locating.Location
import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.compiler.graph.Access
import eu.nitok.jitsu.compiler.graph.AccessImpl
import eu.nitok.jitsu.compiler.graph.Accessor
import eu.nitok.jitsu.compiler.model.BiOperator
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed interface Expression : Element {
    val isConstant: ReasonedBoolean;
    fun calculateType(context: Map<String, Type>, messages: CompilerMessages, typeHint: Type? = null): Type?
    val location: Location
    val type: Type

    @Serializable
    data class Undefined(override val location: Location) : Expression {
        @Transient
        override val isConstant: ReasonedBoolean = ReasonedBoolean.False("No value is defined")
        override fun calculateType(context: Map<String, Type>, messages: CompilerMessages, typeHint: Type?): Type {
            return type
        }

        override val type = Type.Undefined

        @Transient
        override val children: List<Element> = listOf()
    }

    @Serializable
    class VariableReference(override val reference: Located<String>) : AccessImpl<Variable>(), Expression, Access.VariableAccess, ScopeAware {
        @Transient override val restore = JitsuModule::getVariable
        @Transient override val getSymbolId: JitsuModule.(Variable) -> SymbolID = JitsuModule::getSymbolID


        override val isConstant: ReasonedBoolean get() = ReasonedBoolean.False("Cuz not implemented yet")
        override fun calculateType(context: Map<String, Type>, messages: CompilerMessages, typeHint: Type?): Type? {
            val type = context[reference.value]
                ?: target?.declaredType
                ?: target?.initialValue?.calculateType(context, messages)
            if (type != null) this.type = type
            return type
        }

        override lateinit var type: Type
            private set;
        override val location: Location get() = reference.location

        override val children: List<Element> get() = listOf()


        override lateinit var accessKind: Access.VariableAccess.AccessKind
    }

    @Serializable
    data class ArrayLiteral(val elements: List<Expression>, override val location: Location) : Expression {
        override val isConstant: ReasonedBoolean
            get() = elements.map { it.isConstant }.reduce { acc, boolean -> acc.and(boolean) }

        override fun calculateType(
            context: Map<String, Type>,
            messages: CompilerMessages,
            typeHint: Type?
        ): Type.Array {
            val type = (if (elements.isEmpty()) {
                if (typeHint is Type.Array) typeHint.elementType else {
                    messages.error("Cannot infer type of empty array", this.location)
                    Type.Undefined
                }
            } else elements.mapNotNull { element ->
                element.calculateType(context, messages, typeHint?.let { it as? Type.Array }?.elementType)
            }.reduce { acc, type ->
                val aToB = acc.acceptsInstanceOf(type)
                if (aToB.value) acc
                else {
                    val bToA = type.acceptsInstanceOf(acc)
                    if (bToA.value) type
                    else Type.Union(listOf(acc, type))
                }
            }).let { Type.Array(it, elements.size, 1) }
            this.type = type
            return type
        }

        override lateinit var type: Type.Array
        override val children: List<Element> get() = elements
    }
}