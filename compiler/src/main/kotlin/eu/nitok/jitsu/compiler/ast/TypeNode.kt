package eu.nitok.jitsu.compiler.ast

import eu.nitok.jitsu.compiler.ast.StatementNode.Declaration.FunctionDeclarationNode
import eu.nitok.jitsu.compiler.model.BitSize
import eu.nitok.jitsu.compiler.parser.Range
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable;

@Serializable
sealed interface TypeNode : AstNode {
    @Serializable
    class IntTypeNode(val bitSize: BitSize, override val location: Range) : TypeNode, AstNodeImpl() {

        override fun toString(): String {
            return "i${bitSize.bits}"
        }

        override val children: List<AstNode>
            get() = listOf()
    }
    @Serializable
    class UIntTypeNode(val bitSize: BitSize, override val location: Range) : TypeNode, AstNodeImpl() {

        override fun toString(): String {
            return "u${bitSize.bits}"
        }

        override val children: List<AstNode>
            get() = listOf()
    }

    @Serializable
    class FloatTypeNode(val bitSize: BitSize, override val location: Range) : TypeNode, AstNodeImpl() {
        override fun toString(): String {
            return "f${bitSize.bits}"
        }

        override val children: List<AstNode>
            get() = listOf()
    }

    @Serializable
    class FunctionTypeSignatureNode(
        val returnType: TypeNode?,
        var parameters: List<FunctionDeclarationNode.ParameterNode>,
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
        val fixedSize: ExpressionNode?,
        override val location: Range
    ) : TypeNode, AstNodeImpl() {
        override val children: List<AstNode>
            get() = listOfNotNull(type, fixedSize)
        override fun toString(): String {
            return "$type[${fixedSize?.toString() ?: ""}]"
        }
    }

    /**
     * This node is used when a type is referenced by name
     * ```
     * type MyType = OtherType
     * var x: Element[]
     * ```
     * * In this example, `OtherType` and `Element` are NamedTypeNodes
     *
     * It might seem counter intuitive but this type is not [NamedTypeNode] by intent! Since it is only a "reference"
     * to one! Given the example above `MyType` would be a [NamedTypeNode]
     */
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
            val type: TypeNode?
        ) : AstNodeImpl() {
            override val location: Range = name.location.rangeTo(type?.location ?: name.location)
            override fun toString(): String {
                return "${name.value}: $type"
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
}