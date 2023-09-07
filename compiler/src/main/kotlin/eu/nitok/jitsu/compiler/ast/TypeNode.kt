package eu.nitok.jitsu.compiler.ast

import eu.nitok.jitsu.compiler.model.BitSize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable;

@Serializable
sealed class TypeNode() {
    abstract val location: Location

    @Serializable
    class IntTypeNode(val bitSize: N<BitSize>, override val location: Location) : TypeNode()

    @Serializable
    class FloatTypeNode(val bitSize: N<BitSize>, override val location: Location) : TypeNode()

    @Serializable
    class StringTypeNode(override val location: Location) : TypeNode()
    @Serializable
    class EnumDeclarationNode(
        val constants: List<N<ConstantNode>>,
        override val location: Location,
        val keywordLocation: Location
    ) : TypeNode() {

        @Serializable
        class ConstantNode(val name: String, val location: Location)
    }

    @Serializable
    class ArrayTypeNode(
        @SerialName("type_definition") val type: N<TypeNode>,
        val fixedSize: N<ExpressionNode>?,
        override val location: Location
    ) : TypeNode() {
    }

    @Serializable
    class NamedTypeNode(
        val name: N<Located<String>>,
        val genericTypes: List<N<TypeNode>>,
        override val location: Location
    ) :
        TypeNode()

    @Serializable
    class UnionTypeNode(val types: List<N<TypeNode>>, override val location: Location) : TypeNode()

    @Serializable
    class ValueTypeNode(val value: N<ExpressionNode>, override val location: Location) : TypeNode()
}