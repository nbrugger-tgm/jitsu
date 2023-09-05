package eu.nitok.jitsu.compiler.ast

import eu.nitok.jitsu.compiler.model.BitSize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable;

@Serializable
sealed class TypeNode() {
    abstract val location: Location

    @Serializable
    class IntTypeNode(val bitSize: BitSize, override val location: Location) : TypeNode()

    @Serializable
    class FloatTypeNode(val bitSize: BitSize, override val location: Location) : TypeNode()

    @Serializable
    class StringTypeNode(override val location: Location) : TypeNode()
    @Serializable
    class EnumDeclarationNode(
        val name: String?,
        val constants: List<ConstantNode>,
        override val location: Location,
        val nameLocation: Location?,
        val keywordLocation: Location
    ) : TypeNode() {

        @Serializable
        class ConstantNode(val name: String, val location: Location)
    }

    @Serializable
    class ArrayTypeNode(
        @SerialName("type_definition") val type: TypeNode,
        val fixedSize: Int?,
        override val location: Location,
        val sizeLocation: Location?
    ) : TypeNode() {
    }

    @Serializable
    class NamedTypeNode(
        val name: String,
        val genericTypes: List<TypeNode>,
        override val location: Location,
        val nameLocation: Location
    ) :
        TypeNode()

    @Serializable
    class UnionTypeNode(val types: List<TypeNode>, override val location: Location) : TypeNode()

    @Serializable
    class ValueTypeNode(val value: ExpressionNode, override val location: Location) : TypeNode()
}