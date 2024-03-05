package eu.nitok.jitsu.compiler.ast

import eu.nitok.jitsu.compiler.ast.StatementNode.SwitchNode.CaseNode.CaseBodyNode
import eu.nitok.jitsu.compiler.parser.Range
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface StatementNode : AstNode {

    @Serializable
    class VariableDeclarationNode(
        val name: IdentifierNode?,
        @SerialName("type_definition")
        val type: TypeNode?,
        val value: ExpressionNode?,
        override val location: Range,
        val keywordLocation: Range
    ) : AstNodeImpl(listOfNotNull(name, type, value)), StatementNode


    @Serializable
    class FunctionDeclarationNode(
        val name: IdentifierNode,
        val parameters: List<ParameterNode>,
        val returnType: TypeNode?,
        val body: CodeBlockNode?,
        val keywordLocation: Range,
        override val attributes: List<AttributeNode>
    ) : AstNodeImpl(parameters + name + listOfNotNull(returnType, body) + attributes), StatementNode, ExpressionNode,
        CanHaveAttributes {
        override val location: Range = keywordLocation.rangeTo(
            body?.location ?: returnType?.location ?: parameters.lastOrNull()?.location ?: name.location
        )

        @Serializable
        class ParameterNode(
            val name: IdentifierNode,
            val type: TypeNode?,
            val defaultValue: ExpressionNode?,
        ) : AstNodeImpl(listOfNotNull(name, type, defaultValue)) {
            override fun toString() = "${name.value} : $type${if (defaultValue != null) " = $defaultValue" else ""}"
            override val location: Range =
                name.location.rangeTo(defaultValue?.location ?: type?.location ?: name.location)
        }
    }

    @Serializable
    class ReturnNode(val expression: ExpressionNode?, override val location: Range, val keywordLocation: Range) :
        AstNodeImpl(listOfNotNull(expression)), StatementNode

    @Serializable
    class IfNode(
        val condition: ExpressionNode?,
        val thenCodeBlockNode: CodeBlockNode?,
        val elseStatement: ElseNode?,
        override val location: Range,
        val keywordLocation: Range
    ) : AstNodeImpl(listOfNotNull(condition, thenCodeBlockNode, elseStatement)), StatementNode, ExpressionNode {
        @Serializable
        sealed interface ElseNode : AstNode {
            val keywordLocation: Range

            @Serializable
            class ElseIfNode(val ifNode: IfNode, override val keywordLocation: Range) : AstNodeImpl(listOf(ifNode)),
                ElseNode {
                override val location: Range
                    get() = keywordLocation.rangeTo(ifNode.location)
            }

            @Serializable
            class ElseBlockNode(val codeBlock: CodeBlockNode?, override val keywordLocation: Range) :
                AstNodeImpl(listOfNotNull(codeBlock)), ElseNode {
                override val location: Range
                    get() = if (codeBlock == null) keywordLocation else keywordLocation.rangeTo(codeBlock.location)
            }
        }
    }

    //    @Serializabledata
    //    class While(val condition: ExpressionNode, val body: CodeBlockNode) : StatementNode()
//    @Serializabledata
//    class For(val variable: VariableDeclaration, val condition: ExpressionNode, val increment: ExpressionNode, val body: CodeBlockNode) : StatementNode()
//    @Serializabledata
//    class Break(val label: String?) : StatementNode()
//    @Serializabledata
//    class Continue(val label: String?) : StatementNode()
//    @Serializabledata
//    class Label(val label: String) : StatementNode()
    @Serializable
    sealed interface CodeBlockNode : StatementNode, ExpressionNode, CaseBodyNode {
        @Serializable
        class SingleExpressionCodeBlock(val expression: ExpressionNode, override val location: Range) :
            AstNodeImpl(listOf(expression)), CodeBlockNode

        @Serializable
        class StatementsCodeBlock(val statements: List<StatementNode>, override val location: Range) :
            AstNodeImpl(statements), CodeBlockNode
    }

    @Serializable
    class SwitchNode(
        val item: ExpressionNode?,
        val cases: List<CaseNode>,
        override val location: Range,
        val keywordLocation: Range
    ) : AstNodeImpl(cases + listOfNotNull(item)), StatementNode, ExpressionNode {
        @Serializable
        abstract class CaseNode(val matcher: CaseMatchNode, val body: CaseBodyNode?, val keywordLocation: Range) :
            AstNode {
            @Serializable
            sealed interface CaseMatchNode : AstNode {
                @Serializable
                class ConstantCaseNode(
                    val value: ExpressionNode,
                    val keywordLocation: Range,
                    override val location: Range
                ) : CaseMatchNode,
                    AstNodeImpl(listOf(value))

                @Serializable
                class TypeCaseNode(
                    @SerialName("type_definition")
                    val type: TypeNode.NameTypeNode,
                    val keywordLocation: Range,
                    override val location: Range
                ) : CaseMatchNode, AstNodeImpl(listOf(type))

                @Serializable
                class DefaultCaseNode(override val location: Range) : CaseMatchNode, AstNodeImpl(listOf())
            }

            @Serializable
            sealed interface CaseBodyNode : AstNode
        }
    }


    sealed interface NamedTypeDeclarationNode : AstNode, StatementNode {
        val name: IdentifierNode;

        @Serializable
        data class TypeAliasNode(
            override val name: IdentifierNode,
            @SerialName("definition") val type: TypeNode,
            override val location: Range,
            val keywordLocation: Range,
            override val attributes: List<AttributeNode>
        ) : NamedTypeDeclarationNode, AstNodeImpl(attributes + name + type), StatementNode, CanHaveAttributes

        @Serializable
        data class EnumDeclarationNode(
            override val name: IdentifierNode,
            val constants: List<IdentifierNode>,
            override val location: Range,
            val keywordLocation: Range
        ) : NamedTypeDeclarationNode, AstNodeImpl(constants + name) {
            override fun toString(): String {
                return "enum {${constants.joinToString(", ") { it.value }}}"
            }
        }

        @Serializable
        data class InterfaceTypeNode(
            override val name: IdentifierNode,
            val functions: List<FunctionSignatureNode>,
            override val location: Range,
            val keywordLocation: Range,
            override val attributes: List<AttributeNode>
        ) : NamedTypeDeclarationNode, AstNodeImpl(functions + attributes + name), CanHaveAttributes, StatementNode {
            override fun toString(): String {
                return name.value
            }

            @Serializable
            class FunctionSignatureNode(
                val name: IdentifierNode,
                val typeSignature: TypeNode.FunctionTypeSignatureNode,
            ) : AstNodeImpl(listOf(name, typeSignature)) {
                override val location: Range get() = name.location.rangeTo(typeSignature.location)
            }
        }
    }

    @Serializable
    class FunctionCallNode(
        val function: IdentifierNode,
        val parameters: List<ExpressionNode>,
        override val location: Range
    ) : AstNodeImpl(parameters + function), StatementNode, ExpressionNode

    @Serializable
    class AssignmentNode(
        val target: AssignmentTarget,
        val value: ExpressionNode,
    ) : StatementNode, AstNodeImpl(listOf(target, value)) {
        @Serializable
        sealed interface AssignmentTarget : AstNode

        override val location: Range
            get() = target.location.rangeTo(value.location)
    }

    @Serializable
    class MethodInvocationNode(
        val method: ExpressionNode.FieldAccessNode,
        val parameters: List<ExpressionNode>,
        override val location: Range
    ) : StatementNode, ExpressionNode, AstNodeImpl(parameters + method)

    @Serializable
    data class LineCommentNode(val content: Located<String>, override val location: Range) : StatementNode,
        AstNodeImpl(listOf())

    @Serializable
    data class YieldStatement(
        val expression: ExpressionNode?,
        val keywordLocation: Range
    ) : StatementNode, AstNodeImpl(listOfNotNull(expression)) {
        override val location: Range
            get() = if (expression == null) keywordLocation else keywordLocation.rangeTo(expression.location)
    }
}