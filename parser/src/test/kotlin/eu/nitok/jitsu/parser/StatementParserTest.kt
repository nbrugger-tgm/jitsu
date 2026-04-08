package eu.nitok.jitsu.parser

import eu.nitok.jitsu.parser.ast.ExpressionNode
import eu.nitok.jitsu.parser.ast.StatementNode
import eu.nitok.jitsu.parser.ast.StatementNode.InstructionNode.ReturnNode
import eu.nitok.jitsu.parser.ast.StatementNode.InstructionNode.VariableDeclarationNode
import eu.nitok.jitsu.parser.ast.TypeNode
import eu.nitok.jitsu.parser.parsers.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.InstanceOfAssertFactories
import org.assertj.core.api.InstanceOfAssertFactories.type
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("Statement Parsing")
class StatementParserTest : ParsingTest() {

    @Nested
    @DisplayName("parseVariableDeclaration()")
    inner class ParseVariableDeclaration : MethodTest<VariableDeclarationNode>() {

        override fun parseMethod(input: String) =
            parseVariableDeclaration(tokenize(input))

        override fun fullyValidInputs() = listOf(
            "var x = 5",
            "var x: i32 = 5",
            "var x: i64 = 5",
            "var myVar: Array<i64> = arr",
            "var result = someFunction",
            "var flag: boolean = true"
        )

        override fun invalidInputs() = listOf(
            "x = 5",        // missing var keyword
            "let x = 5",    // wrong keyword
            "const x = 5",  // wrong keyword
            "val x = 5",    // wrong keyword
            "= 5",          // no keyword at all
            "42",           // literal, not a statement
        )

        override fun partiallyValidInputs() = listOf(
            Input("var = 5", 1),     // missing name → 1 error
            Input("var x", 2),       // missing = and value → 2 errors
            Input("var x =", 1),     // missing value → 1 error
            Input("var x: i32", 2),  // missing = and value → 2 errors
            Input("var x: = 12", 1), // missing type
        )

        @Test
        fun shouldParseName() {
            val result = parseVariableDeclaration(tokenize("var myVar = 5"))
            assertThat(result)
                .isNotNull()
                .extracting { it!!.name?.value }
                .isEqualTo("myVar")
        }

        @Test
        fun shouldParseTypeWhenPresent() {
            val result = parseVariableDeclaration(tokenize("var x: i32 = 5"))
            assertThat(result)
                .isNotNull()
            assertThat(result?.type)
                .isNotNull()
                .isInstanceOf(TypeNode.IntTypeNode::class.java)
        }

        @Test
        fun shouldHaveNullTypeWhenAbsent() {
            val result = parseVariableDeclaration(tokenize("var x = 5"))
            assertThat(result)
                .isNotNull()
            assertThat(result?.type)
                .isNull()
        }

        @Test
        fun shouldParseInitializerExpression() {
            val result = parseVariableDeclaration(tokenize("var x = 42"))
            assertThat(result)
                .isNotNull()
            assertThat(result?.value)
                .isNotNull()
        }

        @Test
        fun shouldAttachErrorWhenNameMissing() {
            val result = parseVariableDeclaration(tokenize("var = 5"))
            assertThat(result).isNotNull()
            assertThat(result?.errors)
                .isNotEmpty()
                .first()
                .extracting { it.message }
                .asString()
                .containsIgnoringCase("name")
        }

        @Test
        fun shouldAttachErrorWhenInitializerMissing() {
            val result = parseVariableDeclaration(tokenize("var x"))
            assertThat(result).isNotNull()
            assertThat(result?.errors)
                .isNotEmpty()
                .anySatisfy { error ->
                    assertThat(error.message).containsIgnoringCase("initial value")
                }
        }

        @Test
        fun shouldReturnNullForWrongKeyword() {
            assertThat(parseVariableDeclaration(tokenize("let x = 5"))).isNull()
            assertThat(parseVariableDeclaration(tokenize("const x = 5"))).isNull()
        }

        @Test
        fun shouldParseGenericType() {
            val result = parseVariableDeclaration(tokenize("var items: Array<i64> = src"))
            assertThat(result).isNotNull()
            assertThat(result?.type)
                .isNotNull()
                .isInstanceOf(TypeNode.NameTypeNode::class.java)
                .extracting { (it as TypeNode.NameTypeNode).name.value }
                .isEqualTo("Array")
        }
    }

    @Nested
    @DisplayName("parseReturnStatement()")
    inner class ParseReturnStatement : MethodTest<ReturnNode>() {

        override fun parseMethod(input: String) =
            parseReturnStatement(tokenize(input)) as? ReturnNode

        override fun fullyValidInputs() = listOf(
            "return",
            "return 42",
            "return someVar",
            "return true",
        )

        override fun invalidInputs() = listOf(
            "ret",
            "42",
            "foo",
            "x = 5",
            "var x = 5",
        )

        // Return is either fully valid (keyword present, expression optional) or null
        override fun partiallyValidInputs() = emptyList<Input>()

        @Test
        fun shouldReturnNullExpressionWhenJustKeyword() {
            val result = parseReturnStatement(tokenize("return"))
                    as? ReturnNode
            assertThat(result).isNotNull()
            assertThat(result?.expression).isNull()
            assertThatNoErrors()
        }

        @Test
        fun shouldParseReturnWithValue() {
            val result = parseReturnStatement(tokenize("return 42"))
                    as? ReturnNode
            assertThat(result).isNotNull()
            assertThat(result?.expression).isNotNull()
        }

        @Test
        fun shouldParseReturnWithVariableReference() {
            val result = parseReturnStatement(tokenize("return myVar"))
                    as? ReturnNode
            assertThat(result).isNotNull()
            assertThat(result?.expression)
                .isNotNull()
                .isInstanceOf(ExpressionNode.VariableReferenceNode::class.java)
                .extracting { (it as ExpressionNode.VariableReferenceNode).variable.value }
                .isEqualTo("myVar")
        }

        @Test
        fun shouldReturnNullForNonReturnKeyword() {
            assertThat(parseReturnStatement(tokenize("yield 5"))).isNull()
            assertThat(parseReturnStatement(tokenize("ret 5"))).isNull()
        }
    }

    @Nested
    @DisplayName("parseAssignment() via parseStatement()")
    inner class ParseAssignment {

        @ParameterizedTest
        @ValueSource(strings = ["x = 5;", "myVar = 42;", "result = someValue;"])
        fun shouldParseSimpleAssignment(input: String) {
            val result = parseStatement(tokenize(input))
            assertThat(result)
                .isNotNull()
                .isInstanceOf(StatementNode.InstructionNode.AssignmentNode::class.java)
        }

        @Test
        fun shouldParseAssignmentTarget() {
            val result = parseStatement(tokenize("myVar = 42;"))
            assertThat(result)
                .isNotNull()
                .asInstanceOf(type(StatementNode.InstructionNode.AssignmentNode::class.java))
                .extracting { it.target }
                .isNotNull()
                .isInstanceOf(ExpressionNode.VariableReferenceNode::class.java)
                .extracting { (it as ExpressionNode.VariableReferenceNode).variable.value }
                .isEqualTo("myVar")
        }

        @Test
        fun shouldParseAssignmentValue() {
            val result = parseStatement(tokenize("x = 99;"))
            assertThat(result)
                .isNotNull()
                .asInstanceOf(type(StatementNode.InstructionNode.AssignmentNode::class.java))
                .extracting { it.value }
                .isNotNull()
        }

        @Test
        fun shouldAttachErrorWhenValueMissing() {
            val result = parseStatement(tokenize("x =;"))
            assertThat(result)
                .isNotNull()
                .isInstanceOf(StatementNode.InstructionNode.AssignmentNode::class.java)
            val assignment = result as StatementNode.InstructionNode.AssignmentNode
            assertThat(assignment.errors)
                .isNotEmpty()
        }

        @Test
        fun shouldParseAssignmentDirectly() {
            val tokens = tokenize("= 5")
            val id = parseIdentifier(tokenize("myVar").skipWhitespace())!!
            val result = parseAssignment(tokens, id)
            assertThat(result).isNotNull()
            assertThat(result?.value).isNotNull()
            assertThat(result?.target)
                .isInstanceOf(ExpressionNode.VariableReferenceNode::class.java)
                .extracting { (it as ExpressionNode.VariableReferenceNode).variable.value }
                .isEqualTo("myVar")
        }

        @Test
        fun shouldReturnNullWhenNoEqualsSign() {
            val id = parseIdentifier(tokenize("myVar").skipWhitespace())!!
            val result = parseAssignment(tokenize("5"), id)
            assertThat(result).isNull()
        }
    }

    @Nested
    @DisplayName("parseFunctionCall() via parseStatement()")
    inner class ParseFunctionCall {

        @ParameterizedTest
        @ValueSource(strings = ["foo();", "bar();", "myFunction();"])
        fun shouldParseNoArgFunctionCall(input: String) {
            val result = parseStatement(tokenize(input))
            assertThat(result)
                .isNotNull()
                .isInstanceOf(StatementNode.InstructionNode.FunctionCallNode::class.java)
        }

        @ParameterizedTest
        @ValueSource(strings = ["foo(1);", "bar(1, 2);", "myFn(a, b, c);"])
        fun shouldParseFunctionCallWithArguments(input: String) {
            val result = parseStatement(tokenize(input))
            assertThat(result)
                .isNotNull()
                .isInstanceOf(StatementNode.InstructionNode.FunctionCallNode::class.java)
        }

        @Test
        fun shouldParseFunctionName() {
            val result = parseStatement(tokenize("myFunc();"))
            assertThat(result)
                .isNotNull()
                .asInstanceOf(type(StatementNode.InstructionNode.FunctionCallNode::class.java))
                .extracting { it.function.value }
                .isEqualTo("myFunc")
        }

        @Test
        fun shouldParseEmptyParameterList() {
            val result = parseStatement(tokenize("foo();"))
            assertThat(result)
                .isNotNull()
                .asInstanceOf(type(StatementNode.InstructionNode.FunctionCallNode::class.java))
                .extracting { it.parameters }
                .asInstanceOf(InstanceOfAssertFactories.LIST)
                .isEmpty()
        }

        @Test
        fun shouldParseSingleParameter() {
            val result = parseStatement(tokenize("foo(42);"))
            assertThat(result)
                .isNotNull()
                .asInstanceOf(type(StatementNode.InstructionNode.FunctionCallNode::class.java))
                .extracting { it.parameters }
                .asInstanceOf(InstanceOfAssertFactories.LIST)
                .hasSize(1)
        }

        @Test
        fun shouldParseMultipleParameters() {
            val result = parseStatement(tokenize("bar(1, 2, 3);"))
            assertThat(result)
                .isNotNull()
                .asInstanceOf(type(StatementNode.InstructionNode.FunctionCallNode::class.java))
                .extracting { it.parameters }
                .asInstanceOf(InstanceOfAssertFactories.LIST)
                .hasSize(3)
        }

        @Test
        fun shouldParseFunctionCallDirectly() {
            val id = parseIdentifier(tokenize("myFunc").skipWhitespace())!!
            val result = parseFunctionCall(tokenize("(1, 2)"), id)
            assertThat(result).isNotNull()
            assertThat(result?.function?.value).isEqualTo("myFunc")
            assertThat(result?.parameters)
                .isNotNull()
                .hasSize(2)
        }

        @Test
        fun shouldReturnNullWhenNoOpeningParenthesis() {
            val id = parseIdentifier(tokenize("myFunc").skipWhitespace())!!
            val result = parseFunctionCall(tokenize("1, 2"), id)
            assertThat(result).isNull()
        }
    }

    @Nested
    @DisplayName("parseStatement()")
    inner class ParseStatementTests {

        @Test
        fun shouldParseVariableDeclarationStatement() {
            val result = parseStatement(tokenize("var x = 5;"))
            assertThat(result)
                .isNotNull()
                .isInstanceOf(VariableDeclarationNode::class.java)
        }

        @Test
        fun shouldParseVariableDeclarationWithType() {
            val result = parseStatement(tokenize("var x: i32 = 5;"))
            assertThat(result)
                .isNotNull()
                .isInstanceOf(VariableDeclarationNode::class.java)
        }

        @Test
        fun shouldParseReturnStatementWithValue() {
            val result = parseStatement(tokenize("return 42;"))
            assertThat(result)
                .isNotNull()
                .isInstanceOf(ReturnNode::class.java)
        }

        @Test
        fun shouldParseReturnStatementWithoutValue() {
            val result = parseStatement(tokenize("return;"))
            assertThat(result)
                .isNotNull()
                .isInstanceOf(ReturnNode::class.java)
        }

        @Test
        fun shouldParseAssignmentStatement() {
            val result = parseStatement(tokenize("x = 5;"))
            assertThat(result)
                .isNotNull()
                .isInstanceOf(StatementNode.InstructionNode.AssignmentNode::class.java)
        }

        @Test
        fun shouldParseFunctionCallStatement() {
            val result = parseStatement(tokenize("foo();"))
            assertThat(result)
                .isNotNull()
                .isInstanceOf(StatementNode.InstructionNode.FunctionCallNode::class.java)
        }

        @Test
        fun shouldAttachErrorWhenSemicolonMissing() {
            val result = parseStatement(tokenize("var x = 5"))
            assertThat(result).isNotNull()
            assertThat(result?.errors)
                .isNotEmpty()
                .anySatisfy { error ->
                    assertThat(error.message).containsIgnoringCase("semicolon")
                }
        }

        @Test
        fun shouldReturnNullForEmptyInput() {
            val result = parseStatement(tokenize(""))
            assertThat(result).isNull()
        }

        @Test
        fun shouldParseTypeDeclarationStatement() {
            val result = parseStatement(tokenize("type MyType = i64;"))
            assertThat(result)
                .isNotNull()
                .isInstanceOf(StatementNode.NamedTypeDeclarationNode::class.java)
        }

        @ParameterizedTest
        @ValueSource(strings = [
            "var x = 5;",
            "var x: i32 = 5;",
            "return;",
            "return 42;",
            "x = 5;",
            "foo();",
            "bar(1, 2);",
        ])
        fun shouldProduceANodeForAllValidStatements(input: String) {
            val result = parseStatement(tokenize(input))
            assertThat(result)
                .`as`("'$input' should parse into a non-null statement node")
                .isNotNull()
        }
    }
}
