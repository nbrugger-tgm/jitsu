package eu.nitok.jitsu.compiler.ast

import kotlinx.serialization.Serializable

@Serializable
sealed class ExpressionNode() {
    abstract val location: Location

    @Serializable
    class StatementExpressionNode(val statement: StatementNode) : ExpressionNode() {
        override val location: Location get() = statement.location
    }

    @Serializable
    sealed class NumberLiteralNode() : ExpressionNode() {
        @Serializable
        //Long because of unsigned values
        class IntegerLiteralNode(val value: String, override val location: Location) : NumberLiteralNode()

        @Serializable
        class FloatLiteralNode(val value: Double, override val location: Location) : NumberLiteralNode()
    }

    @Serializable
    class StringLiteralNode(
        val content: List<StringPart>, override val location: Location
    ) : ExpressionNode() {
        @Serializable
        sealed interface StringPart {
            @Serializable
            class Literal(val literal: String, val nameLocation: Location, val keywordLocation: Location) : StringPart

            @Serializable
            class Expression(
                val expression: ExpressionNode,
                val startKeywordLocation: Location,
                val endKeywordLocation: Location
            ) : StringPart

            @Serializable
            class Charsequence(val value: String, val location: Location) : StringPart

            @Serializable
            class EscapeSequence(val value: String, val location: Location) : StringPart
        }
    }

    @Serializable
    class BooleanLiteralNode(val value: Boolean, override val location: Location) : ExpressionNode()

    @Serializable
    class VariableLiteralNode(val name: String, override val location: Location) : ExpressionNode()

    @Serializable
    class OperationNode(
        val left: ExpressionNode,
        val operator: BiOperator,
        val right: ExpressionNode,
        override val location: Location,
        val operatorLocation: Location
    ) : ExpressionNode() {
    }

    @Serializable
    class FieldAccessNode(
        val target: ExpressionNode,
        val field: String,
        override val location: Location,
        val fieldLocation: Location
    ) : ExpressionNode() {}

    @Serializable
    class IndexAccessNode(
        val target: ExpressionNode,
        val index: ExpressionNode,
        override val location: Location
    ) : ExpressionNode() {}
}