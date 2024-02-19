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
    ) : AstNodeImpl(), StatementNode


    @Serializable
    class FunctionDeclarationNode(
        val name: IdentifierNode?,
        val parameters: List<ParameterNode>,
        val returnType: TypeNode?,
        val body: CodeBlockNode?,
        override val location: Range,
        val keywordLocation: Range,
        override val attributes: List<AttributeNodeImpl>
    ) : AstNodeImpl(), StatementNode, ExpressionNode, CanHaveAttributes {
        @Serializable
        class ParameterNode(
            val name: IdentifierNode,
            val type: TypeNode?,
            val defaultValue: ExpressionNode?,
            val location: Range
        ) {
            override fun toString(): String {
                return "${name.value} : $type${if (defaultValue != null) " = $defaultValue" else ""}"
            }
        }
    }

    @Serializable
    class ReturnNode(val expression: ExpressionNode?, override val location: Range, val keywordLocation: Range) :
        AstNodeImpl(), StatementNode

    @Serializable
    class IfNode(
        val condition: ExpressionNode?,
        val thenCodeBlockNode: CodeBlockNode?,
        val elseStatement: ElseNode?,
        override val location: Range,
        val keywordLocation: Range
    ) : AstNodeImpl(), StatementNode, ExpressionNode {
        @Serializable
        sealed interface ElseNode : AstNode {
            val keywordLocation: Range

            @Serializable
            class ElseIfNode(val ifNode: IfNode, override val keywordLocation: Range) : AstNodeImpl(), ElseNode {
                override val location: Range
                    get() = keywordLocation.rangeTo(ifNode.location)
            }

            @Serializable
            class ElseBlockNode(val codeBlock: CodeBlockNode?, override val keywordLocation: Range) :
                AstNodeImpl(), ElseNode {
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
            AstNodeImpl(), CodeBlockNode

        @Serializable
        class StatementsCodeBlock(val statements: List<StatementNode>, override val location: Range) :
            AstNodeImpl(), CodeBlockNode
    }

    @Serializable
    class SwitchNode(
        val item: ExpressionNode?,
        val cases: List<CaseNode>,
        override val location: Range,
        val keywordLocation: Range
    ) : AstNodeImpl(), StatementNode, ExpressionNode {
        @Serializable
        abstract class CaseNode(val matcher: CaseMatchNode, val body: CaseBodyNode?, val keywordLocation: Range) :
            AstNode {
            @Serializable
            sealed interface CaseMatchNode : AstNode {
                @Serializable
                class ConstantCaseNode(val value: ExpressionNode, override val location: Range) : CaseMatchNode,
                    AstNodeImpl()

                @Serializable
                class ConditionCaseNode(
                    @SerialName("type_definition")
                    val type: TypeNode,
                    val matching: CaseMatchingNode?,
                    override val location: Range
                ) : CaseMatchNode, AstNodeImpl() {
                    @Serializable
                    sealed class CaseMatchingNode {
                        abstract val location: Range

                        @Serializable
                        class DeconstructPatternMatch(
                            val variables: List<IdentifierNode>, override val location: Range
                        ) : CaseMatchingNode()

                        @Serializable
                        class CastingPatternMatch(
                            val captureName: String, override val location: Range
                        ) : CaseMatchingNode()
                    }
                }

                @Serializable
                class DefaultCaseNode(override val location: Range) : CaseMatchNode,AstNodeImpl()
            }

            @Serializable
            sealed interface CaseBodyNode : AstNode
        }
    }

    @Serializable
    class TypeDefinitionNode(
        val name: IdentifierNode?,
        @SerialName("definition") val type: TypeNode?,
        override val location: Range,
        val keywordLocation: Range,
        override val attributes: List<AttributeNodeImpl>
    ) : AstNodeImpl(), StatementNode, CanHaveAttributes


    @Serializable
    class FunctionCallNode(
        val function: IdentifierNode,
        val parameters: List<ExpressionNode>,
        override val location: Range
    ) : AstNodeImpl(), StatementNode, ExpressionNode

    @Serializable
    class AssignmentNode(
        val target: AssignmentTarget,
        val value: ExpressionNode,
    ) : StatementNode,AstNodeImpl() {
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
    ) : StatementNode, ExpressionNode, AstNodeImpl()

    @Serializable
    data class LineCommentNode(val content: Located<String>, override val location: Range) : StatementNode, AstNodeImpl() {}

    @Serializable
    data class YieldStatement(
        val expression: ExpressionNode?,
        val keywordLocation: Range
    ) : StatementNode,AstNodeImpl() {
        override val location: Range
            get() = if(expression == null) keywordLocation else keywordLocation.rangeTo(expression.location)
    }
}