package eu.nitok.jitsu.compiler.ast

import eu.nitok.jitsu.compiler.ast.StatementNode.AssignmentNode.AssignmentTarget
import eu.nitok.jitsu.compiler.parser.Range
import kotlinx.serialization.Serializable

@Serializable
sealed interface ExpressionNode : AstNode {

    @Serializable
    sealed interface NumberLiteralNode : ExpressionNode {
        @Serializable
        //Long because of unsigned values
        class IntegerLiteralNode(val value: String, override val location: Range) : AstNodeImpl(),
            NumberLiteralNode {
            override fun toString(): String {
                return value
            }
        }

        @Serializable
        class FloatLiteralNode(val value: Double, override val location: Range) : AstNodeImpl(), NumberLiteralNode {
            override fun toString(): String {
                return value.toString() + "f"
            }
        }
    }

    @Serializable
    class StringLiteralNode(
        val content: List<StringPart>
    ) : ExpressionNode, AstNodeImpl() {
        override val location: Range get() = content.first().location.rangeTo(content.last().location)

        @Serializable
        sealed interface StringPart : AstNode {
            @Serializable
            class Literal(
                val literal: String,
                val nameLocation: Range,
                val keywordLocation: Range
            ) : AstNodeImpl(), StringPart {
                override val location: Range get() = keywordLocation.rangeTo(nameLocation)

                override fun toString(): String {
                    return "\$$literal"
                }
            }

            @Serializable
            class Expression(
                val expression: ExpressionNode?,
                val startKeywordLocation: Range,
                val endKeywordLocation: Range
            ) : AstNodeImpl(), StringPart {
                override val location: Range get() = startKeywordLocation.rangeTo(endKeywordLocation)

                override fun toString(): String {
                    return "\${ $expression }"
                }
            }

            @Serializable
            class CharSequence(val value: String, override val location: Range) : AstNodeImpl(), StringPart {
                override fun toString(): String {
                    return value
                }
            }

            @Serializable
            class EscapeSequence(val value: String, override val location: Range) : AstNodeImpl(), StringPart {
                override fun toString(): String {
                    return value
                }
            }
        }

        override fun toString(): String {
            return "\"${content.joinToString("") { it.toString() }}\""
        }
    }

    @Serializable
    class BooleanLiteralNode(val value: Boolean, override val location: Range) : AstNodeImpl(), ExpressionNode {
        override fun toString(): String {
            return value.toString()
        }
    }

    @Serializable
    class VariableLiteralNode(val name: String, override val location: Range) :
        AstNodeImpl(), ExpressionNode, AssignmentTarget {
        override fun toString(): String {
            return name
        }
    }

    @Serializable
    class OperationNode(
        val left: ExpressionNode,
        val operator: Located<BiOperator>?,
        val right: ExpressionNode?,
        override val location: Range
    ) : AstNodeImpl(), ExpressionNode

    @Serializable
    class FieldAccessNode(
        val target: ExpressionNode,
        val field: IdentifierNode?,
        override val location: Range
    ) : AstNodeImpl(), ExpressionNode, AssignmentTarget

    @Serializable
    class IndexAccessNode(
        val target: ExpressionNode,
        val index: ExpressionNode?,
        override val location: Range
    ) : AstNodeImpl(), ExpressionNode, AssignmentTarget
}