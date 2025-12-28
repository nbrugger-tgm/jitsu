package eu.nitok.jitsu.compiler.parser

import eu.nitok.jitsu.compiler.parser.parsers.parseIdentifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("parseIdentifier()")
class IdentifierParserTest : ParsingTest() {

    @Test
    fun parseJustLetterIdentifier() {
        val txt = tokenize("someidentifier")
        val identifier = parseIdentifier(txt)!!
        assertThat(identifier.value).isEqualTo("someidentifier")
        assertThat(identifier.location).isEqualTo(Range(1, 1, 14,1))
        assertThat(identifier.errors).isEmpty();
    }

    @Test
    fun parseIdentifierWithUpperCase() {
        val txt = tokenize("SomeIdentifier")
        val identifier = parseIdentifier(txt)!!
        assertThat(identifier.value).isEqualTo("SomeIdentifier")
        assertThat(identifier.location).isEqualTo(Range(1, 1, 14,1))
        assertThat(identifier.errors).isEmpty();
    }

    @Test
    fun parseIdentifierWithUnderscore() {
        val txt = tokenize("some_identifier")
        val identifier = parseIdentifier(txt)!!
        assertThat(identifier.value).isEqualTo("some_identifier")
        assertThat(identifier.location).isEqualTo(Range(1, 1, 15,1))
        assertThat(identifier.errors).isEmpty();
    }

    @Test
    fun parseIdentifierWithDollar() {
        val txt = tokenize("some\$identifier")
        val identifier = parseIdentifier(txt)!!
        assertThat(identifier.value).isEqualTo("some\$identifier")
        assertThat(identifier.location).isEqualTo(Range(1,1, 15,1))
        assertThat(identifier.errors).isEmpty();
    }

    @Test
    fun parseIdentifierWithNumber() {
        val txt = tokenize("some1identifier")
        val identifier = parseIdentifier(txt)!!
        assertThat(identifier.value).isEqualTo("some1identifier")
        assertThat(identifier.location).isEqualTo(Range(1,1, 15,1))
        assertThat(identifier.errors).isEmpty();
    }

    @Test
    fun parseIdentifierWithAllAllowedCharClasses() {
        val txt = tokenize("some1_identifier\$")
        val identifier = parseIdentifier(txt)!!
        assertThat(identifier.value).isEqualTo("some1_identifier\$")
        assertThat(identifier.location).isEqualTo(Range(1, 1, 17, 1))
        assertThat(identifier.errors).isEmpty();
    }


    @Test
    fun parseIdentifierWithInvalidStart() {
        val txt = tokenize("1someidentifier")
        val identifier = parseIdentifier(txt)!!
        assertThat(identifier.value).isEqualTo("1someidentifier")
        assertThat(identifier.location).isEqualTo(Range(1,1, 15,1))
        assertThat(identifier.errors).hasSize(1)
    }

    @Test
    fun doesNotParseIdentifierWithInvalidChar() {
        val txt = tokenize("]invalidStart");
        val identifier = parseIdentifier(txt);
       assertThat(identifier).isNull()
    }

    @Test
    fun warnWhenIdentifierHasDollar() {
        val txt = tokenize("some\$identifier");
        val identifier = parseIdentifier(txt)!!
        assertThat(identifier.value).isEqualTo("some\$identifier");
        assertThat(identifier.location).isEqualTo(Range(1, 1, 15, 1));
        assertThat(identifier.warnings).hasSize(1);
        assertThat(identifier.errors).isEmpty();
    }

    @Test
    fun parsesUntilNonIdentifier() {
        val txt = tokenize("someidentifier otheridentifier");
        val identifier = parseIdentifier(txt)!!;
        assertThat(identifier.value).isEqualTo("someidentifier");
        assertThat(identifier.location).isEqualTo(Range(1, 1, 14, 1));
        assertThat(identifier.errors).isEmpty();
    }
}