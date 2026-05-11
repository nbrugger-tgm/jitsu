package eu.nitok.jitsu.parser.ast

import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.common.locating.Location
import eu.nitok.jitsu.compiler.model.BiOperator
import kotlinx.serialization.Serializable

sealed interface ExpressionNode : AstNode {
        sealed interface NumberLiteralNode : ExpressionNode {
                class IntegerLiteralNode(val value: String, override val location: Location) : AstNodeImpl(),
            NumberLiteralNode {
            override val children: List<AstNode>
                get() = listOf()
            override fun toString(): String {
                return value
            }
        }

                class FloatLiteralNode(val value: Double, override val location: Location) : AstNodeImpl(),
            NumberLiteralNode {
            override val children: List<AstNode>
                get() = listOf()
            override fun toString(): String {
                return value.toString() + "f"
            }
        }
    }

        data class ArrayLiteralNode(
        val openKw: Location,
        val elements: List<ExpressionNode>,
        val closeKw: Location?
    ): AstNodeImpl(), ExpressionNode {
        override val location: Location = openKw.rangeTo(closeKw?:elements.lastOrNull()?.location?:openKw)
        override val children: List<AstNode> get() = elements
    }

        class StringLiteralNode(
        val content: List<StringPart>,
        override val location: Location
    ) : ExpressionNode, AstNodeImpl() {
        override val children: List<AstNode>
            get() = listOf()

                sealed interface StringPart : AstNode {

                        data class VarReference(
                val literal: VariableReferenceNode?,
                val keywordLocation: Location
            ) : AstNodeImpl(), StringPart {
                override val children: List<AstNode>
                    get() = listOfNotNull(literal)
                override val location: Location get() = keywordLocation.rangeTo(literal?.location ?: keywordLocation)

                override fun toString(): String {
                    return "\$$literal"
                }
            }

                        data class Expression(
                val expression: ExpressionNode?,
                val startKeywordLocation: Location,
                val endKeywordLocation: Location?
            ) : AstNodeImpl(), StringPart {
                override val children: List<AstNode>
                    get() = listOfNotNull(expression)
                override val location: Location get() = startKeywordLocation.rangeTo(endKeywordLocation?:expression?.location?:startKeywordLocation)

                override fun toString(): String {
                    return "\${ $expression }"
                }
            }

                        data class CharSequence(val value: String, override val location: Location) : AstNodeImpl(),
                StringPart {
                override val children: List<AstNode>
                    get() = listOf()
                override fun toString(): String {
                    return value
                }
            }

                        data class EscapeSequence(val escapedCharacter: String, override val location: Location) : AstNodeImpl(),
                StringPart {
                override val children: List<AstNode>
                    get() = listOf()
                override fun toString(): String {
                    return "\\$escapedCharacter"
                }
            }
        }

        override fun toString(): String {
            return "\"${content.joinToString("") { it.toString() }}\""
        }
    }

        class BooleanLiteralNode(val value: Boolean, override val location: Location) : AstNodeImpl(), ExpressionNode {
        override val children: List<AstNode>
            get() = listOf()
        override fun toString(): String {
            return value.toString()
        }
    }

        class VariableReferenceNode(val variable: IdentifierNode) :
        AstNodeImpl(), ExpressionNode, StatementNode.InstructionNode.AssignmentNode.AssignmentTarget {
        override val children: List<AstNode>
            get() = listOf(variable)
        override val location: Location = variable.location
        override fun toString(): String {
            return variable.value
        }
    }

        class OperationNode(
        val left: ExpressionNode,
        val operator: Located<BiOperator>,
        val right: ExpressionNode?
    ) : AstNodeImpl(), ExpressionNode {
        override val children: List<AstNode>
            get() = listOfNotNull(left, right)
        override val location: Location = left.location.rangeTo(right?.location?: operator.location)
        override fun toString(): String {
            return "($left ${operator.value.rune} $right)"
        }
    }

        class FieldAccessNode(
        val target: ExpressionNode,
        val field: IdentifierNode?,
        override val location: Location
    ) : AstNodeImpl(), ExpressionNode, StatementNode.InstructionNode.AssignmentNode.AssignmentTarget {
        override val children: List<AstNode>
            get() = listOfNotNull(target, this.field)
    }

        class IndexAccessNode(
        val target: ExpressionNode,
        val index: ExpressionNode?,
        override val location: Location
    ) : AstNodeImpl(), ExpressionNode, StatementNode.InstructionNode.AssignmentNode.AssignmentTarget {
        override val children: List<AstNode>
            get() = listOfNotNull(target, index)
    }
}
