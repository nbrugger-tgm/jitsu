package eu.nitok.jitsu.parser;

import com.niton.jainparse.token.DefaultToken
import com.niton.jainparse.token.TokenSource
import com.niton.jainparse.token.TokenStream
import eu.nitok.jitsu.common.walk
import eu.nitok.jitsu.parser.ast.AstNode
import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.parser.tokenization.FileTokenStream
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.io.StringReader
import java.net.URI
import java.net.URL

abstract class ParsingTest {
    protected var messages = CompilerMessages();

    @AfterEach
    fun resetMessages() {
        messages.errors.clear()
        messages.warnings.clear()
    }

    protected fun assertThatNoErrors(){
        assertThat(messages.errors).isEmpty()
    }
    protected fun assertThatNoWarnings(){
        assertThat(messages.warnings).isEmpty()
    }

    val url = URI("memory://test.jit")

    protected fun tokenize(txt: String): Tokens {
        val tokens = TokenSource(StringReader(txt), DefaultToken.entries.toTypedArray());
        val tokenStream = TokenStream.of(tokens)
        return FileTokenStream(url, tokenStream)
    }
    abstract inner class MethodTest<T: AstNode> {
        protected abstract fun parseMethod(input: String):T?
        protected abstract fun fullyValidInputs(): List<String>
        /**
         * Inputs that should produce null when parsed since it is not the input the parser is designed to handle
         */
        protected abstract fun invalidInputs(): List<String>
        protected abstract fun partiallyValidInputs(): List<Input>

        @TestFactory
        @DisplayName("valid")
        fun testFullyValidInputs() = fullyValidInputs().map { input ->
            DynamicTest.dynamicTest("'$input' produces no errors") {
                messages.errors.clear()
                messages.warnings.clear()
                val output = parseMethod(input)
                output?.walk {
                    messages.warnings.addAll(it.warnings)
                    messages.errors.addAll(it.errors)
                    true
                }
                assertThat(output)
                    .`as`("The input '$input' should be fully valid and produce a valid AST node")
                    .isNotNull()
                assertThatNoErrors()
                assertThatNoWarnings()
                assertThat(messages.errors)
                    .`as`("The input '$input' should be fully valid and produce no errors in the AST node")
                    .isEmpty()
                assertThat(messages.warnings)
                    .`as`("The input '$input' should be fully valid and produce no warnings in the AST node")
                    .isEmpty()
            }
        }

        @TestFactory
        @DisplayName("invalid")
        fun testInvalidInputs() = invalidInputs().map { input ->
            DynamicTest.dynamicTest("'$input' produces null") {
                val output = parseMethod(input)
                assertThat(output)
                    .`as`("The input '$input' is invalid and produce null")
                    .isNull()
            }
        }

        @TestFactory
        @DisplayName("partially valid")
        fun testPartiallyValidInputs() = partiallyValidInputs().map { (input, expectedErrors) ->
            DynamicTest.dynamicTest("'$input' produces $expectedErrors errors") {
                messages.errors.clear()
                messages.warnings.clear()
                val output = parseMethod(input)
                assertThat(output)
                    .`as`("The input '$input' is partially valid and produce a valid AST node with errors attached")
                    .isNotNull()
                output?.walk {
                    messages.warnings.addAll(it.warnings)
                    messages.errors.addAll(it.errors)
                    true
                }
                assertThat(messages.errors)
                    .`as`("The input '$input' should produce $expectedErrors errors")
                    .hasSize(expectedErrors)
            }
        }
    }
    data class Input(val input: String, val expectedErrors: Int)
}
