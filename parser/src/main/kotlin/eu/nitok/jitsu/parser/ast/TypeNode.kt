package eu.nitok.jitsu.parser.ast

import eu.nitok.jitsu.common.BitSize
import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.common.locating.Location
import eu.nitok.jitsu.parser.ast.ExpressionNode.NumberLiteralNode.IntegerLiteralNode
import kotlinx.serialization.SerialName

sealed interface TypeNode : AstNode {
    sealed interface PrimitiveTypeNode : TypeNode {
        val bitSize: BitSize
    }
        class IntTypeNode(override val bitSize: BitSize, override val location: Location) : PrimitiveTypeNode, AstNodeImpl() {

        override fun toString(): String {
            return "i${bitSize.bits}"
        }

        override val children: List<AstNode>
            get() = listOf()
    }
        class UIntTypeNode(override val bitSize: BitSize, override val location: Location) : PrimitiveTypeNode, AstNodeImpl() {

        override fun toString(): String {
            return "u${bitSize.bits}"
        }

        override val children: List<AstNode>
            get() = listOf()
    }

        class FloatTypeNode(override val bitSize: BitSize, override val location: Location) : PrimitiveTypeNode, AstNodeImpl() {
        override fun toString(): String {
            return "f${bitSize.bits}"
        }

        override val children: List<AstNode>
            get() = listOf()
    }

        class FunctionTypeSignatureNode(
        val returnType: TypeNode?,
        var parameters: List<StatementNode.Declaration.FunctionDeclarationNode.ParameterNode>,
        override val location: Location
    ) : TypeNode, AstNodeImpl() {
        override val children: List<AstNode>
            get() = parameters + listOfNotNull(returnType)
        override fun toString(): String {
            return "(${
                parameters.joinToString(", ") { it.type?.toString() ?: "<invalid>" }
            }) -> $returnType"
        }
    }

        class ArrayTypeNode(
        @SerialName("type_definition") val type: TypeNode,
        val fixedSize: IntegerLiteralNode?,
        override val location: Location
    ) : TypeNode, AstNodeImpl() {
        override val children: List<AstNode>
            get() = listOfNotNull(type, fixedSize)
        override fun toString(): String {
            return "$type[${fixedSize?.toString() ?: ""}]"
        }
    }

        class NameTypeNode(
        val name: IdentifierNode,
        val genericTypes: List<TypeNode>,
        override val location: Location
    ) : TypeNode, AstNodeImpl() {
        override fun toString(): String {
            return "$name${if (genericTypes.isNotEmpty()) "<${genericTypes.joinToString(", ")}>" else ""}"
        }

        override val children: List<AstNode>
            get() = genericTypes + name
    }

        data class UnionTypeNode(val types: List<TypeNode>) : TypeNode, AstNodeImpl() {
        override val children: List<AstNode>
            get() = types

        init {
            require(types.isNotEmpty()) {
                "union types cannot be empty"
            }
        }

        override val location: Location = types.first().location.rangeTo(types.last().location)
        override fun toString(): String {
            return types.joinToString(" | ")
        }
    }

        class StructuralInterfaceTypeNode(
        val fields: List<StructuralFieldNode>,
        override val location: Location
    ) : TypeNode, AstNodeImpl() {
        override val children: List<AstNode>
            get() = fields

        override fun toString(): String {
            return "{${fields.joinToString(", ") { it.toString() }}}"
        }

                class StructuralFieldNode(
            val name: IdentifierNode,
            val type: TypeNode?,
            val mutableKw: Location?,
            val visibility: Located<String>?
        ) : AstNodeImpl() {
            override val location: Location = name.location.rangeTo(type?.location ?: name.location)
            override fun toString(): String {
                return "${if(mutableKw != null)"mut " else ""}${name.value}: $type"
            }

            override val children: List<AstNode>
                get() = listOfNotNull(name, type)
        }
    }

        class ValueTypeNode(val value: ExpressionNode, override val location: Location) : TypeNode, AstNodeImpl() {
        override fun toString(): String {
            return value.toString()
        }

        override val children: List<AstNode>
            get() = listOf(value)
    }

        class NullTypeNode(override val location: Location) : TypeNode, AstNodeImpl() {
        override val children: List<AstNode>
            get() = listOf()
    }

        class BooleanTypeNode(override val location: Location): PrimitiveTypeNode, AstNodeImpl() {
        override val bitSize: BitSize = BitSize.BIT_1
        override val children = emptyList<AstNode>()
    }
}