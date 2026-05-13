package eu.nitok.jitsu.parser

import eu.nitok.jitsu.parser.ast.StatementNode.InstructionNode.CodeBlockNode
import eu.nitok.jitsu.parser.ast.StatementNode.InstructionNode.CodeBlockNode.StatementsCodeBlock
import eu.nitok.jitsu.parser.parsers.parseCodeBlock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("General Parsing")
class GeneralParserTest : ParsingTest() {

    @Nested
    @DisplayName("parseCodeBlock()")
    inner class ParseCodeBlock : MethodTest<StatementsCodeBlock>() {
        override fun parseMethod(input: String) = parseCodeBlock(tokenize(input))

        override fun fullyValidInputs() = listOf(
            "{}",
            "{ }",
            "{  }",
            "{ var x = 5; }",
            "{ return 42; }",
            "{ var x = 5; return x; }"
        )

        override fun invalidInputs() = listOf(
            "var x = 5;",
            "()",
            "[]"
        )

        override fun partiallyValidInputs() = listOf(
            Input("{", 1),
            Input("{ var x = 5;", 1),
            Input("{ var x = 5", 2),
            Input("{ var x = 5 return x;", 2) //missing ; + missing }
        )

        @Test
        fun parsesEmptyCodeBlock() {
            val block = parseCodeBlock(tokenize("{}"))
            assertThat(block).isNotNull()
            assertThat(block?.statements).isEmpty()
        }

        @Test
        fun parsesCodeBlockWithSingleStatement() {
            val block = parseCodeBlock(tokenize("{ var x = 5; }"))
            assertThat(block).isNotNull()
            assertThat(block?.statements).hasSize(1)
        }

        @Test
        fun parsesCodeBlockWithMultipleStatements() {
            val block = parseCodeBlock(tokenize("{ var x = 5; return x; }"))
            assertThat(block).isNotNull()
            assertThat(block?.statements).hasSize(2)
        }

        @Test
        fun attachesErrorForUnclosedBlock() {
            val block = parseCodeBlock(tokenize("{"))
            assertThat(block).isNotNull()
            assertThat(block?.errors).isNotEmpty()
        }
    }
}
