package eu.nitok.jitsu.compiler.ast

import kotlinx.serialization.Serializable

@Serializable
sealed interface ExpressionNode {
    val location: Location

    val located get() = this to location

    @Serializable
    sealed class NumberLiteralNode() : ExpressionNode {
        @Serializable
        //Long because of unsigned values
        class IntegerLiteralNode(val value: String, override val location: Location) : NumberLiteralNode() {
            override fun toString(): String {
                return value
            }
        }

        @Serializable
        class FloatLiteralNode(val value: Double, override val location: Location) : NumberLiteralNode() {
            override fun toString(): String {
                return value.toString()+"f"
            }
        }
    }

    @Serializable
    class StringLiteralNode(
        val content: List<N<StringPart>>, override val location: Location
    ) : ExpressionNode {
        @Serializable
        sealed interface StringPart {
            abstract val location: Location
            @Serializable
            class Literal(val literal: String, val nameLocation: Location, val keywordLocation: Location) : StringPart {
                override val location: Location get() = keywordLocation.rangeTo(nameLocation)

                override fun toString(): String {
                    return "\$$literal"
                }
            }

            @Serializable
            class Expression(
                val expression: N<ExpressionNode>,
                val startKeywordLocation: Location,
                val endKeywordLocation: Location
            ) : StringPart {
                override val location: Location get() = startKeywordLocation.rangeTo(endKeywordLocation)

                override fun toString(): String {
                    return "\${ ... }"
                }
            }

            @Serializable
            class Charsequence(val value: String, override val location: Location) : StringPart {
                override fun toString(): String {
                    return value
                }
            }

            @Serializable
            class EscapeSequence(val value: String, override val location: Location) : StringPart {
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
    class BooleanLiteralNode(val value: Boolean, override val location: Location) : ExpressionNode {
        override fun toString(): String {
            return value.toString()
        }
    }

    @Serializable
    class VariableLiteralNode(val name: String, override val location: Location) : ExpressionNode {
        override fun toString(): String {
            return name
        }
    }

    @Serializable
    class OperationNode(
        val left: N<ExpressionNode>,
        val operator: Located<BiOperator>,
        val right: N<ExpressionNode>,
        override val location: Location
    ) : ExpressionNode {}

    @Serializable
    class FieldAccessNode(
        val target: N<ExpressionNode>,
        val field: N<Located<String>>,
        override val location: Location
    ) : ExpressionNode {}

    @Serializable
    class IndexAccessNode(
        val target: N<ExpressionNode>,
        val index: N<ExpressionNode>,
        override val location: Location
    ) : ExpressionNode {}
}