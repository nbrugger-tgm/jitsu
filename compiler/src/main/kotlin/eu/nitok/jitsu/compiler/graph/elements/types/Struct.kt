package eu.nitok.jitsu.compiler.graph.elements.types

import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.compiler.graph.api.Access
import eu.nitok.jitsu.compiler.graph.api.Element
import eu.nitok.jitsu.compiler.graph.api.Type
import eu.nitok.jitsu.compiler.graph.api.TypeDefinition.ParameterizedType
import eu.nitok.jitsu.compiler.graph.elements.AccessibleElement
import eu.nitok.jitsu.compiler.graph.elements.JitsuModule
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.Boolean
import kotlin.Int

@Serializable
internal data class Struct(
    override val name: Located<String>,
    override val generics: List<TypeParameterElement>,
    override val fields: Set<Field>,
    override val embedded: Set<Lazy<Struct>> = setOf()
) : ParameterizedTypeElement(), ParameterizedType.Struct {

    @Serializable
    internal data class Field(override val name: Located<String>, override var mutable: Boolean, val typeElement: TypeElement) :
        ParameterizedType.Struct.Field, AccessibleElement<ParameterizedType.Struct.Field> {
        override val type: Type get() = typeElement.asType
        override val children: List<Element> get() = listOf(type)

        override var symbolIndex: Int? = null
        @Transient override lateinit var module: JitsuModule
        override fun getSymbol(module: JitsuModule): Int {
            TODO("Not yet implemented")
        }

        @Transient
        override val accessToSelf: MutableList<Access<ParameterizedType.Struct.Field>> = mutableListOf()
    }

    override val allFields: Set<Field> get() = embedded.flatMap { it.value.allFields }.toSet() + fields
    override val children: List<Element> get() = fields.toList() + embedded.map { it.value }
    override fun toType(
        typeParameters: Map<String, TypeElement>
    ): TypeElement {
        TODO("Not yet implemented")
    }
}