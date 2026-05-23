package eu.nitok.jitsu.compiler.graph.elements.types

import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.compiler.graph.api.Element
import eu.nitok.jitsu.compiler.graph.api.Expression
import eu.nitok.jitsu.compiler.graph.api.Type
import eu.nitok.jitsu.compiler.graph.elements.ConstantElement
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal data class Value(val valueElement: ConstantElement<@Contextual Any>) : TypeElement(), Type.Value {
    override val value: Expression.Constant<@Contextual Any> get() = valueElement.asConstant
    override fun rawType(resolveGeneric: ResolveGenericFn?): TypeElement = this
    override fun accepts(type: Type): ReasonedBoolean {
        return if (type is Value && value == type.value) ReasonedBoolean.True("$value is the same as ${type.value}")
        else if (type is Value) ReasonedBoolean.True("$value is not the same as ${type.value}")
        else ReasonedBoolean.True("$type is not a value type, but $this is")
    }

    @Transient
    override val children: List<Element> = listOf(value)
    override fun toString(): String {
        return value.value.toString()
    }
}