package eu.nitok.jitsu.parser

import eu.nitok.jitsu.parser.ast.AttributeDeclarationNode
import eu.nitok.jitsu.parser.ast.AttributeNode
import eu.nitok.jitsu.parser.ast.ExpressionNode
import eu.nitok.jitsu.parser.ast.TypeNode
import eu.nitok.jitsu.parser.parsers.parseAttributeDeclaration
import eu.nitok.jitsu.parser.parsers.parseAttributeUse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.InstanceOfAssertFactories.type
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class AttributeParserTest : ParsingTest() {
    @Nested
    @DisplayName("parseAttributeUse")
    inner class ParseUse : MethodTest<AttributeNode>() {
        override fun parseMethod(input: String): AttributeNode? {
            return parseAttributeUse(tokenize(input))
        }

        override fun fullyValidInputs(): List<String> = listOf(
            "[test]", "[test()]", "[test(test=1)]",
            "[test(test=true)]",
            "[test(test=\"text\")]",
            "[ test ]",
            "[   test  ( ) ]",
            "[ test ( test = 1 ) ]",
            "[ test ( test = true ) ]",
            "[ test ( test = \"te  xt\" ) ]",
            "[test(a=1, b=2, c=\"\")]",
            "[test(  a=1, b=2    , c      =\"\")]",
            "[ \n test \n ( \n a \n = \n 1 \n , \n  b \n = \n 2 \n ,  \n c \n = \n \"\" \n ) \n ]"
        )

        override fun invalidInputs(): List<String> = listOf(
            "(test)", "<test>", "<test)", "(test>", "(test]",
            "test)", "test>", "test]",
        )

        override fun partiallyValidInputs(): List<Input> = listOf(
            Input("[]", 1),
            Input("[", 2),
            Input("[test", 1),
            Input("[test()", 1),
            Input("[test(param=1)", 1),
            Input("[1est", 2),
            Input("[test(", 2),
            Input("[test(param=1", 2),
            Input("[test(param=)", 2),
            Input("[test(=)", 2),
            Input("[(3=", 5),//no attribute name, property as number, no value, closing ), closing ]
        )

        @ParameterizedTest
        @ValueSource(strings = ["1", "!", "    [", "1", "text]"])
        fun shouldReturnNullForAnyInputNotStartingWithSquareBracket(input: String) {
            assertThat(parseMethod(input)).isNull()
        }

        @ParameterizedTest
        @ValueSource(strings = ["[1]", "[!", "[", "[1 2 3 5 7 4 1 2 3", "[]", "[}", "[fun x"])
        fun shouldNotReturnNullForAnyInputStartingWithSquareBracket(input: String) {
            assertThat(parseMethod(input)).isNotNull()
        }

        @Test
        fun shouldParseAttributeWithoutParametersWithoutWarningOrError() {
            val ast = parseMethod("[attributeName]")
            assertThat(ast).isNotNull()
            assertThat(ast?.errors).isEmpty()
            assertThat(ast?.warnings).isEmpty()
        }

        @Test
        fun shouldParseAttributeWithEmptyParameterListWithoutError() {
            val ast = parseMethod("[attributeName()]")
            assertThat(ast).isNotNull()
            assertThat(ast?.errors).isEmpty()
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource(
            value = [
                "[test], test",
                "[test()], test",
                "[test(prop=1)], test",
                "[attr], attr",
                "[attest1], attest1",
                "[compl1cat\$edNam3], compl1cat\$edNam3",
                "[compl1cat\$edNam3()], compl1cat\$edNam3"
            ]
        )
        fun shouldReturnAttributeWithCorrectName(code: String, expectedName: String) {
            val ast = parseMethod(code)
            assertThat(ast?.name?.value)
                .isNotNull()
                .isEqualTo(expectedName)
        }

        @Test
        fun shouldReturnEmptyPropertiesIfNoParanteses() {
            val ast = parseMethod("[attributeName]")
            assertThat(ast?.values)
                .isNotNull()
                .isEmpty()
        }

        @Test
        fun shouldReturnEmptyPropertiesIfEmptyParanteses() {
            val ast = parseMethod("[attributeName()]")
            assertThat(ast?.values)
                .isNotNull()
                .isEmpty()
        }

        @Test
        fun shouldReturnPropertiesFromWithinParanteses() {
            val ast = parseMethod("[attributeName(a=1,b=2)]")
            assertThat(ast?.values)
                .isNotNull()
                .hasSize(2)
                .first()
                .satisfies({
                    assertThat(it.name.value).isEqualTo("a")
                    assertThat(it.value)
                        .asInstanceOf(type(ExpressionNode.NumberLiteralNode.IntegerLiteralNode::class.java))
                        .extracting { it.value }
                        .isEqualTo("1")
                })
            assertThat(ast?.values)
                .element(1)
                .satisfies({
                    assertThat(it.name.value).isEqualTo("b")
                    assertThat(it.value)
                        .asInstanceOf(type(ExpressionNode.NumberLiteralNode.IntegerLiteralNode::class.java))
                        .extracting { it.value }
                        .isEqualTo("2")
                })
        }

        @Test
        fun shouldReturnPropertiesWithoutValuesFromWithinParanteses() {
            val ast = parseMethod("[attributeName(a=,b=2)]")
            assertThat(ast?.values)
                .isNotNull()
                .hasSize(2)
                .first()
                .satisfies({
                    assertThat(it.name.value).isEqualTo("a")
                    assertThat(it.value).isNull()
                })
            assertThat(ast?.values)
                .element(1)
                .satisfies({
                    assertThat(it.name.value).isEqualTo("b")
                    assertThat(it.value)
                        .asInstanceOf(type(ExpressionNode.NumberLiteralNode.IntegerLiteralNode::class.java))
                        .extracting { it.value }
                        .isEqualTo("2")
                })
        }
    }

    @Nested
    @DisplayName("parseAttributeDeclaration")
    inner class ParseDeclaration : MethodTest<AttributeDeclarationNode>() {
        override fun parseMethod(input: String): AttributeDeclarationNode? {
            return parseAttributeDeclaration(tokenize(input), listOf())
        }

        override fun fullyValidInputs(): List<String> = listOf(
            "attribute Test", "attribute Test{}", "attribute Test {}", "attribute Test {\n\n}",
            "attribute Test2{ test:t1; test2: t2; }",
            "attribute Test3{ test:t1; }",
            "attribute Test4 {\n\ttest2: t2;\t}"
        )

        override fun invalidInputs(): List<String> = listOf(
            "Test",
            "class Test {}",
            "[Test]",
            "fn Test() {}"
        )

        override fun partiallyValidInputs(): List<Input> = listOf(
            Input("attribute", 1),
            Input("attribute Test {", 1),
            Input("attribute Test { prop; }", 1),
            Input("attribute Test { prop: ; }", 1),
            Input("attribute Test { prop: t1 }", 1),
            Input("attribute Test { : t1; }", 1)
        )

        @ParameterizedTest
        @ValueSource(strings = ["Test", "class Test {}", "[test]", "fn test() {}", " type Test = i64"])
        fun shouldReturnNullForAnyInputNotStartingWithAttributeKeyword(input: String) {
            assertThat(parseMethod(input)).isNull()
        }

        @ParameterizedTest
        @ValueSource(
            strings = [
                "attribute", "attribute Test", "attribute Test {}",
                "attribute Test { prop:t1; }", "attribute 123", "attribute {", "attribute a lot unrelated text"
            ]
        )
        fun shouldNotReturnNullForAnyInputStartingWithAttributeKeyword(input: String) {
            assertThat(parseMethod(input)).isNotNull()
        }

        @Test
        fun shouldParseAttributeDeclarationWithoutBodyWithoutWarningOrError() {
            val ast = parseMethod("attribute AttributeName")
            assertThat(ast).isNotNull()
            assertThat(ast?.errors).isEmpty()
            assertThat(ast?.warnings).isEmpty()
        }

        @Test
        fun shouldParseAttributeDeclarationWithEmptyBodyWithoutWarningOrError() {
            val ast = parseMethod("attribute AttributeName {}")
            assertThat(ast).isNotNull()
            assertThat(ast?.errors).isEmpty()
            assertThat(ast?.warnings).isEmpty()
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource(
            value = [
                "attribute Test, Test",
                "attribute Test {}, Test",
                "attribute Test { prop:t1; }, Test",
                "attribute Attest1, Attest1",
                "attribute Compl1cat\$edNam3 {}, Compl1cat\$edNam3"
            ]
        )
        fun shouldReturnAttributeDeclarationWithCorrectName(code: String, expectedName: String) {
            val ast = parseMethod(code)
            assertThat(ast?.name?.value)
                .isNotNull()
                .isEqualTo(expectedName)
        }

        @Test
        fun shouldReturnEmptyPropertiesIfNoBodyIsPresent() {
            val ast = parseMethod("attribute AttributeName")
            assertThat(ast?.properties)
                .isNotNull()
                .isEmpty()
        }

        @Test
        fun shouldReturnEmptyPropertiesIfBodyIsEmpty() {
            val ast = parseMethod("attribute AttributeName {}")
            assertThat(ast?.properties)
                .isNotNull()
                .isEmpty()
        }

        @Test
        fun shouldReturnPropertiesFromWithinBody() {
            val ast = parseMethod("attribute AttributeName { a:t1; b: t2; }")
            assertThat(ast?.properties)
                .isNotNull()
                .hasSize(2)
                .first()
                .satisfies({
                    assertThat(it.name?.value).isEqualTo("a")
                    assertThat(it.type)
                        .asInstanceOf(type(TypeNode.NameTypeNode::class.java))
                        .extracting { it.name.value }
                        .isEqualTo("t1")
                })
            assertThat(ast?.properties)
                .element(1)
                .satisfies({
                    assertThat(it.name?.value).isEqualTo("b")
                    assertThat(it.type)
                        .asInstanceOf(type(TypeNode.NameTypeNode::class.java))
                        .extracting { it.name.value }
                        .isEqualTo("t2")
                })
        }

    }
}