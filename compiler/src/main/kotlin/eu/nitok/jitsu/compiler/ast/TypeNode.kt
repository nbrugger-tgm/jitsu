package eu.nitok.jitsu.compiler.ast

import eu.nitok.jitsu.compiler.model.BitSize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable;

@Serializable
sealed class TypeNode() {
    abstract val location: Location

    @Serializable
    class IntTypeNode(val bitSize: N<BitSize>, override val location: Location) : TypeNode() {
        override fun toString(): String {
            return "i${bitSize.map { it.bits }}"
        }
    }

    @Serializable
    class FloatTypeNode(val bitSize: N<BitSize>, override val location: Location) : TypeNode() {
        override fun toString(): String {
            return "f${bitSize.map { it.bits }}"
        }
    }

    @Serializable
    class InterfaceTypeNode(
        val name: N<Located<String>>?,
        val functions: List<N<FunctionSignatureNode>>,
        override val location: Location,
        val keywordLocation: Location,
        override val attributes: List<N<AttributeNode>>
    ) : TypeNode(), CanHaveAttributes, StatementNode{
        override fun toString(): String {
            return name?.map { it.first }?.toString()?: "anonymous interface"
        }

        @Serializable
        class FunctionSignatureNode(
            val name: N<Located<String>>,
            val typeSignature: FunctionTypeSignatureNode,
            val location: Location = com.niton.parser.token.Location.range(
                name.location,
                typeSignature.location
            )
        )
    }


    @Serializable
    class FunctionTypeSignatureNode(
        val returnType: N<TypeNode>?,
        var parameters: List<N<StatementNode.FunctionDeclarationNode.ParameterNode>>,
        override val location: Location
    ) : TypeNode() {
        override fun toString(): String {
            return "(${
                parameters.joinToString(", ") { it.map { it.type }.toString() }
            }) -> $returnType"
        }
    }

    @Serializable
    class StringTypeNode(override val location: Location) : TypeNode() {
        override fun toString(): String {
            return "string"
        }
    }

    @Serializable
    class EnumDeclarationNode(
        val constants: List<N<ConstantNode>>,
        override val location: Location,
        val keywordLocation: Location
    ) : TypeNode() {

        @Serializable
        class ConstantNode(val name: String, val location: Location)

        override fun toString(): String {
            return "enum {${constants.joinToString(", ") { it.map { it.name }.toString() }}}"
        }
    }

    @Serializable
    class ArrayTypeNode(
        @SerialName("type_definition") val type: N<TypeNode>,
        val fixedSize: N<ExpressionNode>?,
        override val location: Location
    ) : TypeNode() {
        override fun toString(): String {
            return "$type[${fixedSize?.toString() ?: ""}]"
        }
    }

    @Serializable
    class NamedTypeNode(
        val name: N<Located<String>>,
        val genericTypes: List<N<TypeNode>>,
        override val location: Location
    ) : TypeNode() {
        override fun toString(): String {
            return "$name${if (genericTypes.isNotEmpty()) "<${genericTypes.joinToString(", ")}>" else ""}"
        }
    }

    @Serializable
    class UnionTypeNode(val types: List<N<TypeNode>>, override val location: Location) : TypeNode() {
        override fun toString(): String {
            return types.joinToString(" | ")
        }
    }

    @Serializable
    class ValueTypeNode(val value: N<ExpressionNode>, override val location: Location) : TypeNode() {
        override fun toString(): String {
            return value.toString()
        }
    }

    @Serializable
    class VoidTypeNode(override val location: Location) : TypeNode()
}