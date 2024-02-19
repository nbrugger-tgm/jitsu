package eu.nitok.jitsu.compiler.ast

import eu.nitok.jitsu.compiler.model.BitSize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable;

@Serializable
sealed interface TypeNode : AstNode {

    @Serializable
    class IntTypeNode(val bitSize: BitSize, override val location: Location) : TypeNode, AstNodeImpl() {
        override fun toString(): String {
            return "i${bitSize.bits}"
        }
    }

    @Serializable
    class FloatTypeNode(val bitSize: BitSize, override val location: Location) : TypeNode, AstNodeImpl() {
        override fun toString(): String {
            return "f${bitSize.bits}"
        }
    }

    @Serializable
    class InterfaceTypeNode(
        val name: IdentifierNode?,
        val functions: List<FunctionSignatureNode>,
        override val location: Location,
        val keywordLocation: Location,
        override val attributes: List<AttributeNodeImpl>
    ) : TypeNode, AstNodeImpl(), CanHaveAttributes, StatementNode {
        override fun toString(): String {
            return name?.value ?: "anonymous interface"
        }

        @Serializable
        class FunctionSignatureNode(
            val name: IdentifierNode,
            val typeSignature: FunctionTypeSignatureNode,
        ) : AstNodeImpl() {
            override val location: Location get() = name.location.rangeTo(typeSignature.location)
        }
    }


    @Serializable
    class FunctionTypeSignatureNode(
        val returnType: TypeNode?,
        var parameters: List<StatementNode.FunctionDeclarationNode.ParameterNode>,
        override val location: Location
    ) : TypeNode, AstNodeImpl() {
        override fun toString(): String {
            return "(${
                parameters.joinToString(", ") { it.type?.toString() ?: "<invalid>" }
            }) -> $returnType"
        }
    }

    @Serializable
    class StringTypeNode(override val location: Location) : TypeNode, AstNodeImpl() {
        override fun toString(): String {
            return "string"
        }
    }

    @Serializable
    class EnumDeclarationNode(
        val constants: List<IdentifierNode>,
        override val location: Location,
        val keywordLocation: Location
    ) : TypeNode, AstNodeImpl() {
        override fun toString(): String {
            return "enum {${constants.joinToString(", ") { it.value }}}"
        }
    }

    @Serializable
    class ArrayTypeNode(
        @SerialName("type_definition") val type: TypeNode,
        val fixedSize: ExpressionNode?,
        override val location: Location
    ) : TypeNode, AstNodeImpl() {
        override fun toString(): String {
            return "$type[${fixedSize?.toString() ?: ""}]"
        }
    }

    @Serializable
    class NamedTypeNode(
        val name: IdentifierNode,
        val genericTypes: List<TypeNode>,
        override val location: Location
    ) : TypeNode, AstNodeImpl() {
        override fun toString(): String {
            return "$name${if (genericTypes.isNotEmpty()) "<${genericTypes.joinToString(", ")}>" else ""}"
        }
    }

    @Serializable
    class UnionTypeNode(val types: List<TypeNode>, override val location: Location) : TypeNode, AstNodeImpl() {
        override fun toString(): String {
            return types.joinToString(" | ")
        }
    }

    @Serializable
    class StructuralInterfaceTypeNode(
        val fields: List<StructuralFieldNode>,
        override val location: Range
    ) : TypeNode, AstNodeImpl(), StatementNode {
        override fun toString(): String {
            return "{${fields.joinToString(", ") { it.toString() }}}"
        }

        @Serializable
        class StructuralFieldNode(
            val name: IdentifierNode,
            val type: TypeNode
        ) : AstNodeImpl() {
            override val location: Range = name.location.rangeTo(type.location)
            override fun toString(): String {
                return "${name.value}: $type"
            }
        }
    }

    @Serializable
    class ValueTypeNode(val value: ExpressionNode, override val location: Range) : TypeNode, AstNodeImpl() {
        override fun toString(): String {
            return value.toString()
        }
    }

    @Serializable
    class VoidTypeNode(override val location: Location) : TypeNode, AstNodeImpl()
}