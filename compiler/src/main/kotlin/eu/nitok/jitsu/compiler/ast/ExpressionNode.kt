package eu.nitok.jitsu.compiler.ast

import eu.nitok.jitsu.compiler.ast.StatementNode.AssignmentNode.AssignmentTarget
import kotlinx.serialization.Serializable

@Serializable
sealed interface ExpressionNode : AstNode {

    @Serializable
    sealed interface NumberLiteralNode : ExpressionNode {
        @Serializable
        //Long because of unsigned values
        class IntegerLiteralNode(val value: String, override val location: Location) : AstNodeImpl(),
            NumberLiteralNode {
            override fun toString(): String {
                return value
            }
        }

        @Serializable
        class FloatLiteralNode(val value: Double, override val location: Location) : AstNodeImpl(), NumberLiteralNode {
            override fun toString(): String {
                return value.toString() + "f"
            }
        }
    }

    @Serializable
    class StringLiteralNode(
        val content: List<StringPart>
    ) : ExpressionNode, AstNodeImpl() {
        override val location: Location get() = content.first().location.rangeTo(content.last().location)

        @Serializable
        sealed interface StringPart : AstNode {
            @Serializable
            class Literal(
                val literal: String,
                val nameLocation: Location,
                val keywordLocation: Location
            ) : AstNodeImpl(), StringPart {
                override val location: Location get() = keywordLocation.rangeTo(nameLocation)

                override fun toString(): String {
                    return "\$$literal"
                }
            }

            @Serializable
            class Expression(
                val expression: ExpressionNode?,
                val startKeywordLocation: Location,
                val endKeywordLocation: Location
            ) : AstNodeImpl(), StringPart {
                override val location: Location get() = startKeywordLocation.rangeTo(endKeywordLocation)

                override fun toString(): String {
                    return "\${ $expression }"
                }
            }

            @Serializable
            class CharSequence(val value: String, override val location: Location) : AstNodeImpl(), StringPart {
                override fun toString(): String {
                    return value
                }
            }

            @Serializable
            class EscapeSequence(val value: String, override val location: Location) : AstNodeImpl(), StringPart {
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
    class BooleanLiteralNode(val value: Boolean, override val location: Location) : AstNodeImpl(), ExpressionNode {
        override fun toString(): String {
            return value.toString()
        }
    }

    @Serializable
    class VariableLiteralNode(val name: String, override val location: Location) :
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
        override val location: Location
    ) : AstNodeImpl(), ExpressionNode

    @Serializable
    class FieldAccessNode(
        val target: ExpressionNode,
        val field: IdentifierNode?,
        override val location: Location
    ) : AstNodeImpl(), ExpressionNode, AssignmentTarget

    @Serializable
    class IndexAccessNode(
        val target: ExpressionNode,
        val index: ExpressionNode?,
        override val location: Location
    ) : AstNodeImpl(), ExpressionNode, AssignmentTarget
}