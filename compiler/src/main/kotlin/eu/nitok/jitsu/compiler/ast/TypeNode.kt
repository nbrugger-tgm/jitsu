package eu.nitok.jitsu.compiler.ast

import eu.nitok.jitsu.compiler.model.BitSize
import eu.nitok.jitsu.compiler.parser.Range
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable;

@Serializable
sealed interface TypeNode : AstNode {
    @Serializable
    class IntTypeNode(val bitSize: BitSize, override val location: Range) : TypeNode, AstNodeImpl(listOf()) {

        override fun toString(): String {
            return "i${bitSize.bits}"
        }
    }

    @Serializable
    class FloatTypeNode(val bitSize: BitSize, override val location: Range) : TypeNode, AstNodeImpl(listOf()) {
        override fun toString(): String {
            return "f${bitSize.bits}"
        }
    }

    @Serializable
    class FunctionTypeSignatureNode(
        val returnType: TypeNode?,
        var parameters: List<StatementNode.FunctionDeclarationNode.ParameterNode>,
        override val location: Range
    ) : TypeNode, AstNodeImpl(parameters + listOfNotNull(returnType)) {
        override fun toString(): String {
            return "(${
                parameters.joinToString(", ") { it.type?.toString() ?: "<invalid>" }
            }) -> $returnType"
        }
    }

    @Serializable
    class StringTypeNode(override val location: Range) : TypeNode, AstNodeImpl(listOf()) {
        override fun toString(): String {
            return "string"
        }
    }

    @Serializable
    class ArrayTypeNode(
        @SerialName("type_definition") val type: TypeNode,
        val fixedSize: ExpressionNode?,
        override val location: Range
    ) : TypeNode, AstNodeImpl(listOfNotNull(type, fixedSize)) {
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
    ) : TypeNode, AstNodeImpl(genericTypes + name) {
        override fun toString(): String {
            return "$name${if (genericTypes.isNotEmpty())"<${genericTypes.joinToString(", ")}>" else ""}"
        }
    }

    @Serializable
    class UnionTypeNode(val types: List<TypeNode>, override val location: Range) : TypeNode, AstNodeImpl(types) {
        override fun toString(): String {
            return types.joinToString(" | ")
        }
    }

    @Serializable
    class StructuralInterfaceTypeNode(
        val fields: List<StructuralFieldNode>,
        override val location: Range
    ) : TypeNode, AstNodeImpl(fields) {
        override fun toString(): String {
            return "{${fields.joinToString(", ") { it.toString() }}}"
        }

        @Serializable
        class StructuralFieldNode(
            val name: IdentifierNode,
            val type: TypeNode
        ) : AstNodeImpl( listOf(name, type)) {
            override val location: Range = name.location.rangeTo(type.location)
            override fun toString(): String {
                return "${name.value}: $type"
            }
        }
    }

    @Serializable
    class ValueTypeNode(val value: ExpressionNode, override val location: Range) : TypeNode, AstNodeImpl(listOf(value)) {
        override fun toString(): String {
            return value.toString()
        }
    }

    @Serializable
    class VoidTypeNode(override val location: Range) : TypeNode, AstNodeImpl(listOf())
}