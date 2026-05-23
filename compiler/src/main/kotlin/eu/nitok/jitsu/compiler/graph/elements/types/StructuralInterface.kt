package eu.nitok.jitsu.compiler.graph.elements.types

import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.compiler.graph.api.Element
import eu.nitok.jitsu.compiler.graph.api.Type
import kotlinx.serialization.Serializable

@Serializable
internal data class StructuralInterface(override val fields: Map<String, Struct.Field>) : TypeElement(), Type.StructuralInterface {
    override fun rawType(resolveGeneric: ResolveGenericFn?): TypeElement {
        return StructuralInterface(fields.mapValues { (_, b) ->
            b.copy(
                typeElement = b.typeElement.rawType(
                    resolveGeneric
                )
            )
        })
    }

    override fun accepts(type: Type): ReasonedBoolean {
        return ReasonedBoolean.False("structural interface assignability not implemented yet")
    }

    override val children: List<Element> get() = fields.values.toList()
    override fun toString(): String {
        return "{${fields.entries.joinToString(", ")}}"
    }
}