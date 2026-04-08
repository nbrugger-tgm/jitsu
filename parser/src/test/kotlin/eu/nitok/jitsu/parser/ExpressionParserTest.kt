package eu.nitok.jitsu.parser

import eu.nitok.jitsu.common.walk
import eu.nitok.jitsu.compiler.model.BiOperator
import eu.nitok.jitsu.parser.ast.ExpressionNode
import eu.nitok.jitsu.parser.ast.ExpressionNode.NumberLiteralNode.IntegerLiteralNode
import eu.nitok.jitsu.parser.ast.ExpressionNode.StringLiteralNode
import eu.nitok.jitsu.parser.ast.ExpressionNode.StringLiteralNode.StringPart
import eu.nitok.jitsu.parser.ast.StatementNode
import eu.nitok.jitsu.parser.parsers.parseExpression
import eu.nitok.jitsu.parser.parsers.parseIntLiteral
import eu.nitok.jitsu.parser.parsers.parseOperation
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.InstanceOfAssertFactories.type
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("Expression Parsing")
class ExpressionParserTest : ParsingTest() {

    @Nested
    @DisplayName("parseIntLiteral()")
    inner class ParseIntLiteral : MethodTest<IntegerLiteralNode>() {

        override fun parseMethod(input: String) = parseIntLiteral(tokenize(input)) as? IntegerLiteralNode

        override fun fullyValidInputs() = listOf(
            "123",
            "0",
            "999999",
            "+100",
            "-42",
            "+ 100",
            "- 42",
        )

        override fun invalidInputs() = listOf(
            "abc",
            "[]",
            "\"hello\"",
            " ",
            "+",
            "+abc",
            "-",
            "-abc",
        )

        // The integer literal parser either succeeds fully or returns null – there are no
        // partially-valid cases (no error-bearing nodes are produced).
        override fun partiallyValidInputs(): List<Input> = emptyList()

        @Test
        fun parsesPositiveInteger() {
            val result = parseIntLiteral(tokenize("42"))
            assertThat(result)
                .isNotNull()
                .isInstanceOf(IntegerLiteralNode::class.java)
                .extracting { (it as IntegerLiteralNode).value }
                .isEqualTo("42")
        }

        @Test
        fun parsesExplicitPlusSign() {
            val result = parseIntLiteral(tokenize("+100"))
            assertThat(result)
                .isNotNull()
                .asInstanceOf(type(IntegerLiteralNode::class.java))
                .extracting { it.value }
                .isEqualTo("+100")
        }

        @Test
        fun parsesNegativeInteger() {
            val result = parseIntLiteral(tokenize("-42"))
            assertThat(result)
                .isNotNull()
                .asInstanceOf(type(IntegerLiteralNode::class.java))
                .extracting { it.value }
                .isEqualTo("-42")
        }

        @Test
        fun parsesZero() {
            val result = parseIntLiteral(tokenize("0"))
            assertThat(result)
                .isNotNull()
                .asInstanceOf(type(IntegerLiteralNode::class.java))
                .extracting { it.value }
                .isEqualTo("0")
        }

        @Test
        fun returnsNullForAlpha() {
            val result = parseIntLiteral(tokenize("abc"))
            assertThat(result).isNull()
        }

        @Test
        fun returnsNullForPlusWithNoNumber() {
            val result = parseIntLiteral(tokenize("+abc"))
            assertThat(result).isNull()
        }

        @Test
        fun locationSpansFromStart() {
            val result = parseIntLiteral(tokenize("-99")) as? IntegerLiteralNode
            assertThat(result).isNotNull()
            assertThat(result!!.location.start.column).isEqualTo(1)
        }

        @ParameterizedTest
        @ValueSource(strings = ["123", "0", "+1", "-1", "+ 0", "- 0"])
        fun parsedValueIsNonNull(input: String) {
            val result = parseIntLiteral(tokenize(input))
            assertThat(result).isNotNull()
        }
    }

    @Nested
    @DisplayName("parseStringLiteral() (via parseExpression)")
    inner class ParseStringLiteral : MethodTest<StringLiteralNode>() {

        override fun parseMethod(input: String) = parseExpression(tokenize(input)) as? StringLiteralNode

        // All properly quoted strings should parse without errors
        override fun fullyValidInputs() = listOf(
            "\"\"",
            "\"hello\"",
            "\"test\\n\"",
            "\"\$name\"",
            "\"Hello \$name\"",
            "\"Hello \${name}\"",
            "\"Hello \${1 + 1}\"",
            "\"Hello \${\"name\"}\"",
            "\"with spaces\"",
            "\"123\"",
            "\"1 + 1 = \${1+1}\"\\n\\t2 + 2 = \$result and \\$ can be used in \\\"text\\\""
        )

        // Non-string inputs should cause parseExpression to return something other
        // than a StringLiteralNode.
        override fun invalidInputs() = listOf(
            "123",
            "abc",
            "[]",
        )

        // Unclosed strings should produce errors but still return a node
        override fun partiallyValidInputs(): List<Input> = listOf(
            Input("\"hello", 1),            // Unclosed string → 1 error
            Input("\"", 1),                 // Just opening quote → 1 error
        )

        @Test
        fun parsesMixedPartsCorrectly(){
            val parsed = parseExpression(
                tokenize("\"1 + 1 = \${1+1}\\n\\t2 + 2 = \$result and \\$ can be used in \\\"text\\\"")
            )!! as StringLiteralNode
            assertThat(parsed.content).isNotEmpty();
            assertThat(parsed.content[0])
                .asInstanceOf(type(StringPart.CharSequence::class.java))
                .extracting  { it.value }
                .isEqualTo("1 + 1 = ")

            assertThat(parsed.content[1])
                .asInstanceOf(type(StringPart.Expression::class.java))
                .extracting  { it.expression?.toString() }
                .isEqualTo("(1 + 1)")

            assertThat(parsed.content[2])
                .asInstanceOf(type(StringPart.EscapeSequence::class.java))
                .extracting  { it.escapedCharacter }
                .isEqualTo("n")

            assertThat(parsed.content[3])
                .asInstanceOf(type(StringPart.EscapeSequence::class.java))
                .extracting  { it.escapedCharacter }
                .isEqualTo("t")

            assertThat(parsed.content[4])
                .asInstanceOf(type(StringPart.CharSequence::class.java))
                .extracting  { it.value }
                .isEqualTo("2 + 2 = ")
            assertThat(parsed.content[5])
                .asInstanceOf(type(StringPart.VarReference::class.java))
                .extracting  { it.literal?.variable?.value }
                .isEqualTo("result")

            assertThat(parsed.content[6])
                .asInstanceOf(type(StringPart.CharSequence::class.java))
                .extracting  { it.value }
                .isEqualTo(" and ")

            assertThat(parsed.content[7])
                .asInstanceOf(type(StringPart.EscapeSequence::class.java))
                .extracting  { it.escapedCharacter }
                .isEqualTo("$")

            assertThat(parsed.content[8])
                .asInstanceOf(type(StringPart.CharSequence::class.java))
                .extracting  { it.value }
                .isEqualTo(" can be used in ")

            assertThat(parsed.content[9])
                .asInstanceOf(type(StringPart.EscapeSequence::class.java))
                .extracting  { it.escapedCharacter }
                .isEqualTo("\"")

            assertThat(parsed.content[10])
                .asInstanceOf(type(StringPart.CharSequence::class.java))
                .extracting  { it.value }
                .isEqualTo("text")
            assertThat(parsed.content[11])
                .asInstanceOf(type(StringPart.EscapeSequence::class.java))
                .extracting  { it.escapedCharacter }
                .isEqualTo("\"")
        }

        @Test
        fun parsesEmptyStringWithoutErrors() {
            val result = parseExpression(tokenize("\"\"")) as? StringLiteralNode
            assertThat(result).isNotNull()
            assertThat(result!!.content).isEmpty()
            assertThatNoErrors()
        }

        @Test
        fun parsesNonEmptyStringWithoutErrors() {
            val result = parseExpression(tokenize("\"hello\"")) as? StringLiteralNode
            assertThat(result)
                .`as`("A StringLiteralNode should be returned for non-empty strings")
                .isNotNull()
            (result as eu.nitok.jitsu.parser.ast.AstNode).walk {
                messages.errors.addAll(it.errors)
                messages.warnings.addAll(it.warnings)
                true
            }
            assertThat(messages.errors)
                .`as`("Valid string \"hello\" should produce no errors")
                .isEmpty()
        }

        @Test
        fun parsesStringWithContent() {
            val result = parseExpression(tokenize("\"hello\"")) as? StringLiteralNode
            assertThat(result).isNotNull()
            assertThat(result!!.content)
                .`as`("String content should not be empty for non-empty string")
                .isNotEmpty()
        }

        @Test
        fun unclosedStringProducesError() {
            val result = parseExpression(tokenize("\"hello"))
            assertThat(result).isNotNull()
            (result as eu.nitok.jitsu.parser.ast.AstNode).walk {
                messages.errors.addAll(it.errors)
                messages.warnings.addAll(it.warnings)
                true
            }
            assertThat(messages.errors)
                .`as`("Unclosed string should produce at least one error")
                .isNotEmpty()
        }

        @Test
        fun returnsNullForNonString() {
            val result = parseExpression(tokenize("123")) as? StringLiteralNode
            assertThat(result).isNull()
        }

        @Test
        fun returnsNullForIdentifier() {
            val result = parseExpression(tokenize("myVar")) as? StringLiteralNode
            assertThat(result).isNull()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // parseExpression
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("parseExpression()")
    inner class ParseExpression {

        @Test
        fun parsesIntegerLiteral() {
            val result = parseExpression(tokenize("42"))
            assertThat(result)
                .isNotNull()
                .isInstanceOf(IntegerLiteralNode::class.java)
        }

        @Test
        fun parsesNegativeIntegerLiteral() {
            val result = parseExpression(tokenize("-7"))
            assertThat(result)
                .isNotNull()
                .isInstanceOf(IntegerLiteralNode::class.java)
                .extracting { (it as IntegerLiteralNode).value }
                .isEqualTo("-7")
        }

        @Test
        fun parsesStringLiteral() {
            val result = parseExpression(tokenize("\"\""))
            assertThat(result)
                .isNotNull()
                .isInstanceOf(StringLiteralNode::class.java)
        }

        @Test
        fun parsesVariableReference() {
            val result = parseExpression(tokenize("myVariable"))
            assertThat(result)
                .isNotNull()
                .isInstanceOf(ExpressionNode.VariableReferenceNode::class.java)
                .extracting { (it as ExpressionNode.VariableReferenceNode).variable.value }
                .isEqualTo("myVariable")
        }

        @Test
        fun parsesFunctionCallWithNoArguments() {
            val result = parseExpression(tokenize("foo()"))
            assertThat(result)
                .isNotNull()
                .isInstanceOf(StatementNode.InstructionNode.FunctionCallNode::class.java)
                .extracting { (it as StatementNode.InstructionNode.FunctionCallNode).function.value }
                .isEqualTo("foo")
        }

        @Test
        fun parsesFunctionCallWithArguments() {
            val result = parseExpression(tokenize("add(1, 2)"))
            assertThat(result)
                .isNotNull()
                .isInstanceOf(StatementNode.InstructionNode.FunctionCallNode::class.java)
            val call = result as StatementNode.InstructionNode.FunctionCallNode
            assertThat(call.function.value).isEqualTo("add")
            assertThat(call.parameters).hasSize(2)
        }

        @Test
        fun parsesSimpleAddition() {
            val result = parseExpression(tokenize("1 + 2"))
            assertThat(result)
                .isNotNull()
                .isInstanceOf(ExpressionNode.OperationNode::class.java)
            val op = result as ExpressionNode.OperationNode
            assertThat(op.operator.value).isEqualTo(BiOperator.ADDITION)
            assertThat(op.left).isInstanceOf(IntegerLiteralNode::class.java)
            assertThat(op.right).isInstanceOf(IntegerLiteralNode::class.java)
        }

        @Test
        fun parsesVariableAddition() {
            val result = parseExpression(tokenize("a + b"))
            assertThat(result)
                .isNotNull()
                .isInstanceOf(ExpressionNode.OperationNode::class.java)
            val op = result as ExpressionNode.OperationNode
            assertThat(op.left).isInstanceOf(ExpressionNode.VariableReferenceNode::class.java)
            assertThat(op.right).isInstanceOf(ExpressionNode.VariableReferenceNode::class.java)
        }

        @Test
        fun parsesChainedOperationsLeftAssociatively() {
            // "a + b * c" is parsed left-associatively as ((a + b) * c)
            val result = parseExpression(tokenize("a + b * c"))
            assertThat(result)
                .isNotNull()
                .isInstanceOf(ExpressionNode.OperationNode::class.java)
            val outer = result as ExpressionNode.OperationNode
            assertThat(outer.left)
                .`as`("Parser is left-associative; the left of the outer op is itself an operation")
                .isInstanceOf(ExpressionNode.OperationNode::class.java)
        }

        @Test
        fun parsesSubtraction() {
            val result = parseExpression(tokenize("10 - 3"))
            assertThat(result)
                .isNotNull()
                .isInstanceOf(ExpressionNode.OperationNode::class.java)
            val op = result as ExpressionNode.OperationNode
            assertThat(op.operator.value).isEqualTo(BiOperator.SUBTRACTION)
        }

        @Test
        fun parsesMultiplication() {
            val result = parseExpression(tokenize("4 * 5"))
            assertThat(result)
                .isNotNull()
                .isInstanceOf(ExpressionNode.OperationNode::class.java)
            val op = result as ExpressionNode.OperationNode
            assertThat(op.operator.value).isEqualTo(BiOperator.MULTIPLICATION)
        }

        @Test
        fun parsesDivision() {
            val result = parseExpression(tokenize("8 / 2"))
            assertThat(result)
                .isNotNull()
                .isInstanceOf(ExpressionNode.OperationNode::class.java)
            val op = result as ExpressionNode.OperationNode
            assertThat(op.operator.value).isEqualTo(BiOperator.DIVISION)
        }

        @Test
        fun returnsNullForPunctuationOnly() {
            val result = parseExpression(tokenize("[]"))
            assertThat(result).isNull()
        }

        @Test
        fun operationWithMissingRightOperandReturnsNodeWithNullRight() {
            // When the right-hand side of an operator is absent the parser still builds an
            // OperationNode, but right is null.
            val result = parseExpression(tokenize("1 + "))
            assertThat(result)
                .`as`("Even with a missing right operand an OperationNode is returned")
                .isNotNull()
                .isInstanceOf(ExpressionNode.OperationNode::class.java)
            val op = result as ExpressionNode.OperationNode
            assertThat(op.right)
                .`as`("Right operand should be null when absent")
                .isNull()
        }

        @Test
        fun operandValuesArePreservedForIntegerAddition() {
            val result = parseExpression(tokenize("3 + 7")) as? ExpressionNode.OperationNode
            assertThat(result).isNotNull()
            assertThat((result!!.left as IntegerLiteralNode).value).isEqualTo("3")
            assertThat((result.right as IntegerLiteralNode).value).isEqualTo("7")
        }

        @Test
        fun parsesOperationWithoutSpaces() {
            val result = parseExpression(tokenize("1+2"))
            assertThat(result)
                .isNotNull()
                .isInstanceOf(ExpressionNode.OperationNode::class.java)
        }

        @Test
        fun parsesOperationWithExtraSpaces() {
            val result = parseExpression(tokenize("1   +   2"))
            assertThat(result)
                .isNotNull()
                .isInstanceOf(ExpressionNode.OperationNode::class.java)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // parseOperation
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("parseOperation()")
    inner class ParseOperation {

        private fun intLiteral(value: String): IntegerLiteralNode =
            parseIntLiteral(tokenize(value)) as IntegerLiteralNode

        @Test
        fun parsesAdditionOperator() {
            val left = intLiteral("1")
            val result = parseOperation(tokenize(" + 2"), left)
            assertThat(result)
                .isNotNull()
                .isInstanceOf(ExpressionNode.OperationNode::class.java)
            val op = result as ExpressionNode.OperationNode
            assertThat(op.operator.value).isEqualTo(BiOperator.ADDITION)
            assertThat(op.left).isSameAs(left)
            assertThat(op.right).isInstanceOf(IntegerLiteralNode::class.java)
        }

        @Test
        fun parsesSubtractionOperator() {
            val left = intLiteral("5")
            val result = parseOperation(tokenize(" - 3"), left)
            assertThat(result)
                .isNotNull()
                .asInstanceOf(type(ExpressionNode.OperationNode::class.java))
                .extracting { it.operator.value }
                .isEqualTo(BiOperator.SUBTRACTION)
        }

        @Test
        fun parsesMultiplicationOperator() {
            val left = intLiteral("4")
            val result = parseOperation(tokenize(" * 5"), left)
            assertThat(result)
                .isNotNull()
                .asInstanceOf(type(ExpressionNode.OperationNode::class.java))
                .extracting { it.operator.value }
                .isEqualTo(BiOperator.MULTIPLICATION)
        }

        @Test
        fun parsesDivisionOperator() {
            val left = intLiteral("8")
            val result = parseOperation(tokenize(" / 2"), left)
            assertThat(result)
                .isNotNull()
                .asInstanceOf(type(ExpressionNode.OperationNode::class.java))
                .extracting { it.operator.value }
                .isEqualTo(BiOperator.DIVISION)
        }

        @Test
        fun returnsNullWhenNoOperatorPresent() {
            val left = intLiteral("1")
            val result = parseOperation(tokenize("abc"), left)
            assertThat(result).isNull()
        }

        @Test
        fun returnsNullForNonOperatorSymbol() {
            val left = intLiteral("1")
            val result = parseOperation(tokenize(".field"), left)
            assertThat(result).isNull()
        }

        @Test
        fun parsesOperationWithoutLeadingWhitespace() {
            val left = intLiteral("3")
            val result = parseOperation(tokenize("+4"), left)
            assertThat(result)
                .isNotNull()
                .isInstanceOf(ExpressionNode.OperationNode::class.java)
        }
    }
}
