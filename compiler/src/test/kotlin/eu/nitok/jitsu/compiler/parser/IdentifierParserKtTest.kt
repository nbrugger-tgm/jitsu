package eu.nitok.jitsu.compiler.parser

import com.niton.jainparse.token.DefaultToken
import com.niton.jainparse.token.TokenSource
import com.niton.jainparse.token.TokenStream
import eu.nitok.jitsu.compiler.parser.parsers.parseIdentifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.StringReader

class IdentifierParserKtTest {
    private fun tokenize(txt: String): Tokens {
        val tokens = TokenSource(StringReader(txt), DefaultToken.entries.toTypedArray());
        val tokenStream = TokenStream.of(tokens)
        return tokenStream
    }

    @Test
    fun parseJustLetterIdentifier() {
        val txt = tokenize("someidentifier")
        val identifier = parseIdentifier(txt)
        assertThat(identifier.value).isEqualTo("someidentifier")
        assertThat(identifier.location).isEqualTo(Range(0,0, 13,0))
        assertThat(identifier.errors).isEmpty();
    }

    @Test
    fun parseIdentifierWithUpperCase() {
        val txt = tokenize("SomeIdentifier")
        val identifier = parseIdentifier(txt)
        assertThat(identifier.value).isEqualTo("SomeIdentifier")
        assertThat(identifier.location).isEqualTo(Range(0,0, 13,0))
        assertThat(identifier.errors).isEmpty();
    }

    @Test
    fun parseIdentifierWithUnderscore() {
        val txt = tokenize("some_identifier")
        val identifier = parseIdentifier(txt)
        assertThat(identifier.value).isEqualTo("some_identifier")
        assertThat(identifier.location).isEqualTo(Range(0,0, 14,0))
        assertThat(identifier.errors).isEmpty();
    }

    @Test
    fun parseIdentifierWithDollar() {
        val txt = tokenize("some\$identifier")
        val identifier = parseIdentifier(txt)
        assertThat(identifier.value).isEqualTo("some\$identifier")
        assertThat(identifier.location).isEqualTo(Range(0,0, 14,0))
        assertThat(identifier.errors).isEmpty();
    }

    @Test
    fun parseIdentifierWithNumber() {
        val txt = tokenize("some1identifier")
        val identifier = parseIdentifier(txt)
        assertThat(identifier.value).isEqualTo("some1identifier")
        assertThat(identifier.location).isEqualTo(Range(0,0, 14,0))
        assertThat(identifier.errors).isEmpty();
    }

    @Test
    fun parseIdentifierWithAllAllowedCharClasses() {
        val txt = tokenize("some1_identifier\$")
        val identifier = parseIdentifier(txt)
        assertThat(identifier.value).isEqualTo("some1_identifier\$")
        assertThat(identifier.location).isEqualTo(Range(0, 0, 17, 0))
        assertThat(identifier.errors).isEmpty();
    }


    @Test
    fun parseIdentifierWithInvalidStart() {
        val txt = tokenize("1someidentifier")
        val identifier = parseIdentifier(txt)
        assertThat(identifier.value).isEqualTo("1someidentifier")
        assertThat(identifier.location).isEqualTo(Range(0,0, 13,0))
        assertThat(identifier.errors).hasSize(1)
    }

    @Test
    fun parsesIdentifierWithInvalidChar() {
        val txt = tokenize("]invalidStart");
        val identifier = parseIdentifier(txt);
        assertThat(identifier.value).isEqualTo("]invalidStart");
        assertThat(identifier.location).isEqualTo(Range(0,0, 12,0));
        assertThat(identifier.errors).hasSize(1);
    }

    @Test
    fun warnWhenIdentifierHasDollar() {
        val txt = tokenize("some\$identifier");
        val identifier = parseIdentifier(txt);
        assertThat(identifier.value).isEqualTo("some\$identifier");
        assertThat(identifier.location).isEqualTo(Range(0, 0, 14, 0));
        assertThat(identifier.warnings).hasSize(1);
        assertThat(identifier.errors).isEmpty();
    }

    @Test
    fun parsesUntilNonIdentifier() {
        val txt = tokenize("someidentifier otheridentifier");
        val identifier = parseIdentifier(txt);
        assertThat(identifier.value).isEqualTo("someidentifier");
        assertThat(identifier.location).isEqualTo(Range(0, 0, 13, 0));
        assertThat(identifier.errors).isEmpty();
    }
}