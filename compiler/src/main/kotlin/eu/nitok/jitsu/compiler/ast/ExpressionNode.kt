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
            override val children: List<AstNode>
                get() = listOf()
            override fun toString(): String {
                return value
            }
        }

        @Serializable
        class FloatLiteralNode(val value: Double, override val location: Range) : AstNodeImpl(),
            NumberLiteralNode {
            override val children: List<AstNode>
                get() = listOf()
            override fun toString(): String {
                return value.toString() + "f"
            }
        }
    }

    @Serializable
    class StringLiteralNode(
        val content: List<StringPart>
    ) : ExpressionNode, AstNodeImpl() {
        override val children: List<AstNode>
            get() = listOf()
        override val location: Range get() = content.first().location.rangeTo(content.last().location)

        @Serializable
        sealed interface StringPart : AstNode {

            /**
             * the `$abc` part in "I am $abc and i am ${me.age} years old"
             */
            @Serializable
            data class Literal(
                val literal: IdentifierNode,
                val keywordLocation: Range
            ) : AstNodeImpl(), StringPart {
                override val children: List<AstNode>
                    get() = listOf(literal)
                override val location: Range get() = keywordLocation.rangeTo(literal.location)

                override fun toString(): String {
                    return "\$$literal"
                }
            }

            /**
             * the `${me.age}` part in "I am $abc and i am ${me.age} years old"
             */
            @Serializable
            data class Expression(
                val expression: ExpressionNode?,
                val startKeywordLocation: Range,
                val endKeywordLocation: Range
            ) : AstNodeImpl(), StringPart {
                override val children: List<AstNode>
                    get() = listOfNotNull(expression)
                override val location: Range get() = startKeywordLocation.rangeTo(endKeywordLocation)

                override fun toString(): String {
                    return "\${ $expression }"
                }
            }

            /**
             * the `I am `, ` and i am ` and ` years old` parts in "I am $abc and i am ${me.age} years old"
             */
            @Serializable
            data class CharSequence(val value: String, override val location: Range) : AstNodeImpl(),
                StringPart {
                override val children: List<AstNode>
                    get() = listOf()
                override fun toString(): String {
                    return value
                }
            }

            @Serializable
            data class EscapeSequence(val value: String, override val location: Range) : AstNodeImpl(),
                StringPart {
                override val children: List<AstNode>
                    get() = listOf()
                override fun toString(): String {
                    return "\\$value"
                }
            }
        }

        override fun toString(): String {
            return "\"${content.joinToString("") { it.toString() }}\""
        }
    }

    @Serializable
    class BooleanLiteralNode(val value: Boolean, override val location: Range) : AstNodeImpl(), ExpressionNode {
        override val children: List<AstNode>
            get() = listOf()
        override fun toString(): String {
            return value.toString()
        }
    }

    @Serializable
    class VariableReferenceNode(val variable: IdentifierNode) :
        AstNodeImpl(), ExpressionNode, AssignmentTarget {
        override val children: List<AstNode>
            get() = listOf(variable)
        override val location: Range = variable.location
        override fun toString(): String {
            return variable.value
        }
    }

    @Serializable
    class OperationNode(
        val left: ExpressionNode,
        val operator: Located<BiOperator>,
        val right: ExpressionNode?
    ) : AstNodeImpl(), ExpressionNode {
        override val children: List<AstNode>
            get() = listOfNotNull(left, right)
        override val location: Range = left.location.rangeTo(right?.location?: operator.location)
    }

    @Serializable
    class FieldAccessNode(
        val target: ExpressionNode,
        val field: IdentifierNode?,
        override val location: Range
    ) : AstNodeImpl(), ExpressionNode, AssignmentTarget {
        override val children: List<AstNode>
            get() = listOfNotNull(target, this.field)
    }

    @Serializable
    class IndexAccessNode(
        val target: ExpressionNode,
        val index: ExpressionNode?,
        override val location: Range
    ) : AstNodeImpl(), ExpressionNode, AssignmentTarget {
        override val children: List<AstNode>
            get() = listOfNotNull(target, index)
    }
}
