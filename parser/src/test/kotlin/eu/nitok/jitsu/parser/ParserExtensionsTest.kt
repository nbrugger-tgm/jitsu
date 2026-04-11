package eu.nitok.jitsu.parser

import com.niton.jainparse.token.DefaultToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory

@DisplayName("Parser Extensions")
class ParserExtensionsTest : ParsingTest() {
    @Nested
    @DisplayName("enclosedRepetition()")
    inner class EnclosedRepetitionTest {

        fun parse(input: String,placeholder: String? = null): List<String>? = tokenize(input).enclosedRepetition(
            start = DefaultToken.LEFT_ANGLE_BRACKET,
            end = DefaultToken.BIGGER,
            delimitter = DefaultToken.COMMA,
            messages = messages,
            subject = "list of identifiers",
            elementName = "element",
            invalidObjectPlaceholder = placeholder
        ) {
            it.attempt(DefaultToken.LETTERS)?.value?.value
        }?.elements

        @TestFactory
        fun shouldHandleInvalidInputs() = listOf(
            "<A B C>" to listOf("A","B","C"),
            "<A,,,B>" to listOf("A","<invalid>","<invalid>","B"),
            "<A,,,>" to listOf("A","<invalid>","<invalid>","<invalid>"),
            "<A,, ,B>" to listOf("A","<invalid>","<invalid>","B"),
            "<A, ,\t ,B>" to listOf("A","<invalid>","<invalid>","B"),
            "<A, ,B>" to listOf("A","<invalid>","B"),
            "<A, ,B" to listOf("A","<invalid>","B"),
            "<A,  B" to listOf("A","B"),
            "<A   B>" to listOf("A","B"),
            "<A   B" to listOf("A","B"),
            "<A" to listOf("A"),
        ).map { (input, expected) ->
            DynamicTest.dynamicTest("Input '$input' parses to $expected") {
                val result = parse(input,"<invalid>")
                assertThat(result)
                    .containsExactlyElementsOf(expected)
                assertThat(messages.errors)
                    .hasSizeGreaterThanOrEqualTo(1)
            }
        }
        @TestFactory
        fun shouldHandleInvalidInputsWithoutPlaceholder() = listOf(
            "<A, ,B>" to listOf("A","B"),
            "<A, ,B" to listOf("A","B"),
            "<A,  B" to listOf("A","B"),
            "<A   B>" to listOf("A","B"),
            "<A   B" to listOf("A","B"),
            "<A" to listOf("A"),
        ).map { (input, expected) ->
            DynamicTest.dynamicTest("Input '$input' parses to $expected") {
                val result = parse(input)
                assertThat(result)
                    .containsExactlyElementsOf(expected)
                assertThat(messages.errors)
                    .hasSizeGreaterThanOrEqualTo(1)
            }
        }

        @TestFactory
        fun shouldParseValidInputs() = listOf(
            "<A,B,C>" to listOf("A","B","C"),
            "<A>" to listOf("A"),
            "<A,B>" to listOf("A","B"),
            "<A, B, C>" to listOf("A","B","C"),
            "<A ,  B , C>" to listOf("A","B","C"),
        ).map { (input, expected) ->
            DynamicTest.dynamicTest("Input '$input' parses to $expected") {
                val result = parse(input)
                assertThat(result)
                    .containsExactlyElementsOf(expected)
            }
        }

    }
}
