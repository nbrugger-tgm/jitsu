package eu.nitok.jitsu.parser.ast

import eu.nitok.jitsu.common.BitSize
import eu.nitok.jitsu.common.Located
import eu.nitok.jitsu.common.Range
import eu.nitok.jitsu.parser.ast.ExpressionNode.NumberLiteralNode.IntegerLiteralNode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface TypeNode : AstNode {
    sealed interface PrimitiveTypeNode : TypeNode {
        val bitSize: BitSize
    }
    @Serializable
    class IntTypeNode(override val bitSize: BitSize, override val location: Range) : PrimitiveTypeNode, AstNodeImpl() {

        override fun toString(): String {
            return "i${bitSize.bits}"
        }

        override val children: List<AstNode>
            get() = listOf()
    }
    @Serializable
    class UIntTypeNode(override val bitSize: BitSize, override val location: Range) : PrimitiveTypeNode, AstNodeImpl() {

        override fun toString(): String {
            return "u${bitSize.bits}"
        }

        override val children: List<AstNode>
            get() = listOf()
    }

    @Serializable
    class FloatTypeNode(override val bitSize: BitSize, override val location: Range) : PrimitiveTypeNode, AstNodeImpl() {
        override fun toString(): String {
            return "f${bitSize.bits}"
        }

        override val children: List<AstNode>
            get() = listOf()
    }

    @Serializable
    class FunctionTypeSignatureNode(
        val returnType: TypeNode?,
        var parameters: List<StatementNode.Declaration.FunctionDeclarationNode.ParameterNode>,
        override val location: Range
    ) : TypeNode, AstNodeImpl() {
        override val children: List<AstNode>
            get() = parameters + listOfNotNull(returnType)
        override fun toString(): String {
            return "(${
                parameters.joinToString(", ") { it.type?.toString() ?: "<invalid>" }
            }) -> $returnType"
        }
    }

    @Serializable
    class ArrayTypeNode(
        @SerialName("type_definition") val type: TypeNode,
        val fixedSize: IntegerLiteralNode?,
        override val location: Range
    ) : TypeNode, AstNodeImpl() {
        override val children: List<AstNode>
            get() = listOfNotNull(type, fixedSize)
        override fun toString(): String {
            return "$type[${fixedSize?.toString() ?: ""}]"
        }
    }

    @Serializable
    class NameTypeNode(
        val name: IdentifierNode,
        val genericTypes: List<TypeNode>,
        override val location: Range
    ) : TypeNode, AstNodeImpl() {
        override fun toString(): String {
            return "$name${if (genericTypes.isNotEmpty()) "<${genericTypes.joinToString(", ")}>" else ""}"
        }

        override val children: List<AstNode>
            get() = genericTypes + name
    }

    @Serializable
    data class UnionTypeNode(val types: List<TypeNode>) : TypeNode, AstNodeImpl() {
        override val children: List<AstNode>
            get() = types

        init {
            require(types.isNotEmpty()) {
                "union types cannot be empty"
            }
        }

        override val location: Range = types.first().location.rangeTo(types.last().location)
        override fun toString(): String {
            return types.joinToString(" | ")
        }
    }

    @Serializable
    class StructuralInterfaceTypeNode(
        val fields: List<StructuralFieldNode>,
        override val location: Range
    ) : TypeNode, AstNodeImpl() {
        override val children: List<AstNode>
            get() = fields

        override fun toString(): String {
            return "{${fields.joinToString(", ") { it.toString() }}}"
        }

        @Serializable
        class StructuralFieldNode(
            val name: IdentifierNode,
            val type: TypeNode?,
            val mutableKw: Range?,
            val visibility: Located<String>?
        ) : AstNodeImpl() {
            override val location: Range = name.location.rangeTo(type?.location ?: name.location)
            override fun toString(): String {
                return "${if(mutableKw != null)"mut " else ""}${name.value}: $type"
            }

            override val children: List<AstNode>
                get() = listOfNotNull(name, type)
        }
    }

    @Serializable
    class ValueTypeNode(val value: ExpressionNode, override val location: Range) : TypeNode, AstNodeImpl() {
        override fun toString(): String {
            return value.toString()
        }

        override val children: List<AstNode>
            get() = listOf(value)
    }

    @Serializable
    class VoidTypeNode(override val location: Range) : TypeNode, AstNodeImpl() {
        override val children: List<AstNode>
            get() = listOf()
    }

    @Serializable
    class BooleanTypeNode(override val location: Range): PrimitiveTypeNode, AstNodeImpl() {
        override val bitSize: BitSize = BitSize.BIT_1
        override val children = emptyList<AstNode>()
    }
}