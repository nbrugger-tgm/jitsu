package eu.nitok.jitsu.compiler.graph.elements.types

import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.compiler.graph.api.Access
import eu.nitok.jitsu.compiler.graph.api.Element
import eu.nitok.jitsu.compiler.graph.api.Type
import eu.nitok.jitsu.compiler.graph.api.TypeDefinition
import eu.nitok.jitsu.compiler.graph.elements.AccessibleElement
import eu.nitok.jitsu.compiler.graph.elements.JitsuModule
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.Int

@Serializable
internal data class Enum(
    override val name: Located<String>,
    override val constants: List<Constant>,
) : DirectTypeDefinitionElement(), TypeDefinition.DirectTypeDefinition.Enum {

    override val children: List<Element> get() = constants
    override fun rawType(
        resolveGeneric: ResolveGenericFn?
    ): TypeElement {
        return this
    }

    override fun accepts(type: Type): ReasonedBoolean {
        return if (type == this) ReasonedBoolean.True("$type is the same enum as $this")
        else if (type is Enum) ReasonedBoolean.False("$type is not the same enum as $this")
        else ReasonedBoolean.False("$type is not an enum")
    }

    init {
        constants.forEach { it.enum = this }
    }

    @Serializable
    internal data class Constant(override val name: Located<String>) :
        TypeDefinition.DirectTypeDefinition.Enum.Constant, AccessibleElement<TypeDefinition.DirectTypeDefinition.Enum.Constant> {
        override val children: List<Element> get() = listOf()

        @Transient
        override val accessToSelf: MutableList<Access<TypeDefinition.DirectTypeDefinition.Enum.Constant>> =
            mutableListOf()

        @Transient
        override lateinit var enum: Enum
        override var symbolIndex: Int? = null
        @Transient
        override lateinit var module: JitsuModule

        override fun getSymbol(module: JitsuModule): Int {
            TODO("Not yet implemented")
        }


    }
}