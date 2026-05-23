package eu.nitok.jitsu.compiler.graph.elements.types

import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.compiler.graph.api.Element
import eu.nitok.jitsu.compiler.graph.api.Type
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*

@Serializable
internal data object Undefined : TypeElement(), Type.Undefined {
    override fun rawType(resolveGeneric: ResolveGenericFn?): TypeElement = this
    override fun accepts(type: Type): ReasonedBoolean {
        return ReasonedBoolean.True("While the UNDEFINED type does not accept any types, the error is to be treated at the source (the type definition) an not its usage")
    }

    @Transient
    override val children: List<Element> = Collections.emptyList()
}