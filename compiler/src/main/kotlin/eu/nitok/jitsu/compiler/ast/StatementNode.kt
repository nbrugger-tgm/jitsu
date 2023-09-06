package eu.nitok.jitsu.compiler.ast

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class StatementNode() {
    abstract val location: Location

    @Serializable
    class VariableDeclarationNode(
        val name: String,
        @SerialName("type_definition")
        val type: TypeNode?,
        val value: ExpressionNode?,
        override val location: Location,
        val nameLocation: Location,
        val keywordLocation: Location
    ) : StatementNode()


    @Serializable
    class FunctionDeclarationNode(
        val name: String?,
        val parameters: List<ParameterNode>,
        val returnType: TypeNode?,
        val body: CodeBlockNode,
        override val location: Location,
        val nameLocation: Location?,
        val keywordLocation: Location
    ) : StatementNode() {
        @Serializable
        class ParameterNode(
            val name: String,
            val type: TypeNode,
            val defaultValue: ExpressionNode?,
            val location: Location,
            val nameLocation: Location
        ) {
        }
    }

    @Serializable
    class ReturnNode(val expression: ExpressionNode?, override val location: Location, val keywordLocation: Location) :
        StatementNode()

    @Serializable
    class IfNode(
        val condition: ExpressionNode,
        val thenCodeBlockNode: CodeBlockNode,
        val elseStatement: ElseNode?,
        override val location: Location,
        val keywordLocation: Location
    ) : StatementNode() {
        @Serializable
        sealed class ElseNode() {
            @Serializable
            class ElseIfNode(val ifNode: IfNode) : ElseNode()

            @Serializable
            class ElseBlockNode(val codeBlock: CodeBlockNode) : ElseNode()
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
    sealed class CodeBlockNode : StatementNode() {
        @Serializable
        class SingleExpressionCodeBlock(val expression: ExpressionNode, override val location: Location) :
            CodeBlockNode()

        @Serializable
        class StatementsCodeBlock(val statements: List<StatementNode>, override val location: Location) :
            CodeBlockNode()
    }

    @Serializable
    class SwitchNode(
        val item: ExpressionNode,
        val cases: List<CaseNode>,
        override val location: Location,
        val keywordLocation: Location
    ) :
        StatementNode() {
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
        val name: String,
        @SerialName("definition") val type: TypeNode,
        override val location: Location,
        val nameLocation: Location,
        val keywordLocation: Location
    ) : StatementNode()


    @Serializable
    class FunctionCallNode(
        val function: String,
        val parameters: List<ExpressionNode>,
        override val location: Location,
        val nameLocation: Location
    ) : StatementNode()

    @Serializable
    class AssignmentNode(
        val target: AssignmentTarget,
        val value: ExpressionNode,
        override val location: Location,
        val nameLocation: Location
    ) : StatementNode() {
        @Serializable
        sealed class AssignmentTarget(){
            @Serializable
            data class VariableAssignment(val name: String, val location: Location) : AssignmentTarget()
            @Serializable
            data class PropertyAssignment(val property: ExpressionNode.FieldAccessNode) : AssignmentTarget()
        }
    }

    @Serializable
    class MethodInvocationNode(
        val target: ExpressionNode,
        var method: String,
        val parameters: List<ExpressionNode>,
        override val location: Location,
        val nameLocation: Location
    ) : StatementNode()
}