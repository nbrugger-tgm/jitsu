package eu.nitok.jitsu.compiler.ast

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class StatementNode() {
    abstract val location: Location

    @Serializable
    class VariableDeclarationNode(
        val name: N<Located<String>>,
        @SerialName("type_definition")
        val type: N<TypeNode>?,
        val value: N<ExpressionNode>?,
        override val location: Location,
        val keywordLocation: Location
    ) : StatementNode()


    @Serializable
    class FunctionDeclarationNode(
        val name: N<Located<String>>?,
        val parameters: List<N<ParameterNode>>,
        val returnType: N<TypeNode>?,
        val body: N<CodeBlockNode>,
        override val location: Location,
        val keywordLocation: Location
    ) : StatementNode(), ExpressionNode {
        @Serializable
        class ParameterNode(
            val name: N<Located<String>>,
            val type: N<TypeNode>,
            val defaultValue: N<ExpressionNode>?,
            val location: Location
        ) {
        }
    }

    @Serializable
    class ReturnNode(val expression: N<ExpressionNode>?, override val location: Location, val keywordLocation: Location) :
        StatementNode()

    @Serializable
    class IfNode(
        val condition: N<ExpressionNode>,
        val thenCodeBlockNode: N<CodeBlockNode>,
        val elseStatement: N<ElseNode>?,
        override val location: Location,
        val keywordLocation: Location
    ) : StatementNode(), ExpressionNode {
        @Serializable
        sealed class ElseNode() {
            abstract val keywordLocation: Location
            abstract val location: Location;
            @Serializable
            class ElseIfNode(val ifNode: N<IfNode>, override val keywordLocation: Location) : ElseNode() {
                override val location: Location
                    get() = com.niton.parser.token.Location.range(keywordLocation, ifNode.location { it.location })
            }

            @Serializable
            class ElseBlockNode(val codeBlock: N<CodeBlockNode>, override val keywordLocation: Location) : ElseNode(){
                override val location: Location
                    get() = com.niton.parser.token.Location.range(keywordLocation, codeBlock.location { it.location })
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
    sealed class CodeBlockNode : StatementNode(), ExpressionNode {
        @Serializable
        class SingleExpressionCodeBlock(val expression: N<ExpressionNode>, override val location: Location) :
            CodeBlockNode()

        @Serializable
        class StatementsCodeBlock(val statements: List<N<StatementNode>>, override val location: Location) :
            CodeBlockNode()
    }

    @Serializable
    class SwitchNode(
        val item: N<ExpressionNode>,
        val cases: List<N<CaseNode>>,
        override val location: Location,
        val keywordLocation: Location
    ) : StatementNode(), ExpressionNode {
        @Serializable
        class CaseNode(val matcher: CaseMatchNode, val body: CaseBodyNode, val keywordLocation: Location) {
            @Serializable
            sealed class CaseMatchNode {
                abstract val location: Location

                @Serializable
                class ConstantCaseNode(val value: ExpressionNode, override val location: Location) : CaseMatchNode()

                @Serializable
                class ConditionCaseNode(
                    @SerialName("type_definition")
                    val type: TypeNode,
                    val matching: CaseMatchingNode,
                    override val location: Location
                ) : CaseMatchNode() {
                    @Serializable
                    sealed class CaseMatchingNode() {
                        abstract val location: Location

                        @Serializable
                        class DeconstructPatternMatch(
                            val variables: List<Variable>, override val location: Location
                        ) : CaseMatchingNode() {
                            @Serializable
                            data class Variable(val name: String, val location: Location)
                        }

                        @Serializable
                        class CastingPatternMatch(
                            val captureName: String, override val location: Location
                        ) : CaseMatchingNode()
                    }
                }

                @Serializable
                class DefaultCaseNode(override val location: Location) : CaseMatchNode()
            }

            @Serializable
            sealed class CaseBodyNode {
                abstract val location: Location

                @Serializable
                class CodeBlockCaseBodyNode(val codeBlock: CodeBlockNode, override val location: Location) :
                    CaseBodyNode()

                @Serializable
                class ExpressionCaseBodyNode(
                    val expression: ExpressionNode, override val location: Location
                ) : CaseBodyNode()
            }
        }
    }

    @Serializable
    class TypeDefinitionNode(
        val name: N<Located<String>>,
        @SerialName("definition") val type: N<TypeNode>,
        override val location: Location,
        val keywordLocation: Location
    ) : StatementNode()


    @Serializable
    class FunctionCallNode(
        val function: N<ExpressionNode>,
        val parameters: List<N<ExpressionNode>>,
        override val location: Location,
        val nameLocation: Location
    ) : StatementNode(), ExpressionNode

    @Serializable
    class AssignmentNode(
        val target: N<AssignmentTarget>,
        val value: N<ExpressionNode>,
        override val location: Location,
        val nameLocation: Location
    ) : StatementNode() {
        @Serializable
        sealed class AssignmentTarget(){
            @Serializable
            data class VariableAssignment(val name: String, val location: Location) : AssignmentTarget()
            @Serializable
            data class PropertyAssignment(val property: N<ExpressionNode.FieldAccessNode>) : AssignmentTarget()
        }
    }

    @Serializable
    class MethodInvocationNode(
        val target: N<ExpressionNode>,
        var method: N<Located<String>>,
        val parameters: List<N<ExpressionNode>>,
        override val location: Location
    ) : StatementNode(), ExpressionNode

    @Serializable
    data class LineCommentNode(val content: Located<String>, override val location: Location) : StatementNode() {}
}