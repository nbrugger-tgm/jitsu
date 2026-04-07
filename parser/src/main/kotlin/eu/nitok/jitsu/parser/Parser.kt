package eu.nitok.jitsu.parser

import com.niton.jainparse.token.DefaultToken
import com.niton.jainparse.token.DefaultToken.*
import com.niton.jainparse.token.TokenSource
import com.niton.jainparse.token.TokenStream
import com.niton.jainparse.token.Tokenizer
import com.niton.jainparse.token.Tokenizer.AssignedToken
import eu.nitok.jitsu.parser.ast.*
import eu.nitok.jitsu.common.CompilerMessage
import eu.nitok.jitsu.common.CompilerMessage.Hint
import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.Located
import eu.nitok.jitsu.common.Location
import eu.nitok.jitsu.common.Range
import eu.nitok.jitsu.parser.parsers.parseIdentifier
import eu.nitok.jitsu.parser.parsers.parseStatements
import java.io.Reader
import java.io.StringReader
import java.net.URI
import kotlin.jvm.optionals.getOrNull

typealias Tokens = TokenStream<DefaultToken>
typealias ParserFn<T> = Tokens.() -> T

fun parseFile(input: Reader, uri: URI): SourceFileNode {
    val tokenSource = TokenSource(input, tokenizer);
    val tokens = TokenStream.of(tokenSource)
    val statements = mutableListOf<StatementNode>()
    val sourceFileNode = SourceFileNode(uri.toString(), statements)
    parseStatements(tokens, statements, sourceFileNode::error)
    return sourceFileNode;
}

fun parseFile(txt: String, uri: URI): SourceFileNode {
    val tokens = tokenize(txt)
    val statements = mutableListOf<StatementNode>()
    val sourceFileNode = SourceFileNode(uri.toString(), statements)
    parseStatements(tokens, statements, sourceFileNode::error)
    return sourceFileNode;
}

fun Tokens.skipUntil(vararg stoppers: DefaultToken): Range {
    return range {
        while (peekOptional().map { !stoppers.contains(it.type) }.orElse(false)) skip()
    }.location
}

fun Tokens.skip(vararg toSkip: DefaultToken) {
    while (this.hasNext() && toSkip.contains(this.peek().type)) {
        this.next()
    }
}

fun Tokens.skip(n: Int = 1) {
    var count = n;
    while (count-- > 0 && hasNext()) {
        this@skip.next()
    }
}

private val tokenizer = Tokenizer(DefaultToken.entries)

private fun tokenize(txt: String): Tokens {
    val tokens = TokenSource(StringReader(txt), tokenizer);
    val tokenStream = TokenStream.of(tokens)
    return tokenStream
}

public fun <T> parseIdentifierBased(tokens: Tokens, parser: (tokens: Tokens, id: IdentifierNode) -> T?): T? {
    tokens.elevate()
    val id = parseIdentifier(tokens) ?: run {
        tokens.rollback()
        return null
    }
    tokens.skipWhitespace()
    val res = parser(tokens, id)
    if (res == null) tokens.rollback()
    else tokens.commit()
    return res
}

sealed interface LastElementState {
    object Delimitted : LastElementState
    data class NotDelimitted(val location: Location) : LastElementState
}

fun <T> Tokens.enclosedRepetition(
    start: DefaultToken,
    delimitter: DefaultToken,
    end: DefaultToken,
    messages: CompilerMessages,
    subject: String,
    elementName: String,
    invalidObjectPlaceholder: T? = null,
    parseElement: (Tokens) -> T?
): List<T>? {
    val openKw = attempt(start)?.location ?: return null;
    val lst = mutableListOf<T>()
    skipWhitespace()
    if (attempt(end) != null) return lst

    var wasLastElementDelimitted: LastElementState = LastElementState.Delimitted
    while (hasNext()) {
        when (val x = parseElement(this)) {
            null -> {
                if(wasLastElementDelimitted is LastElementState.NotDelimitted) {
                    //The last element was not followed by a delimitter, therefore an invalid element here means
                    // Its more probable that the user forgot the "end" mark rather than that they forgot a delimitter
                    break
                }
                skip(WHITESPACE)
                val invalid = skipUntil(end, delimitter, NEW_LINE, SEMICOLON)
                messages.error(CompilerMessage("Expected a $elementName", invalid))
                val next = peekOptional().getOrNull()?.type
                if(invalidObjectPlaceholder != null && (next == end || next == delimitter))
                    lst.add(invalidObjectPlaceholder)
                if (next != delimitter) {
                    break;
                }
                skip(1)
                wasLastElementDelimitted = LastElementState.Delimitted
                skipWhitespace()
            }

            else -> {
                if(wasLastElementDelimitted is LastElementState.NotDelimitted) {
                    messages.error(
                        "Expected a '$delimitter' between $elementName elements",
                        wasLastElementDelimitted.location.toRange()
                    )
                }
                lst.add(x)
                val expectedDelimiterPos = location
                skipWhitespace()
                val next = peekOptional().getOrNull()?.type
                wasLastElementDelimitted = when(next) {
                    delimitter -> {
                        skip() //consume delimitter
                        skipWhitespace()
                        LastElementState.Delimitted
                    }
                    end -> break
                    else -> LastElementState.NotDelimitted(expectedDelimiterPos)
                }
            }
        }
    }

    skipWhitespace()

    this@enclosedRepetition.attempt(end) ?: messages.error(
        "Unclosed $subject, expected $end", location.toRange(), Hint(
            "$subject started here", openKw
        )
    )
    return lst
}

fun Tokens.skipWhitespace():Tokens {
    skip(WHITESPACE, NEW_LINE)
    return this
}

inline fun <T> Tokens.range(action: ParserFn<T>): Located<T> {
    val start = location
    val res = action()
    val end = lastConsumedLocation
    return Located(res, start.rangeTo(end))
}

inline fun <T> Tokens.nullableRange(action: ParserFn<T?>): Located<T>? {
    val start = location
    val res = action()
    if (res == null) return null
    val end = lastConsumedLocation
    return Located(res, start.rangeTo(end))
}

fun Tokens.attempt(vararg tokens: DefaultToken): Located<AssignedToken<DefaultToken>>? {
    val next = peekOptional().getOrNull() ?: return null;
    if (tokens.contains(next.type)) {
        return range { this.next() }
    }
    return null;
}

fun <T> Tokens.attempt(function: ParserFn<T>): T? {
    if (!hasNext()) {
        return null
    }
    elevate()
    val res = this.function()
    if (res == null) {
        rollback()
    } else {
        commit()
    }
    return res;
}

fun Tokens.keyword(s: String): Range? {
    return attempt {
        val (token, location) = range { nextOptional().getOrNull() }
        return@attempt if (token == null) null
        else if (token.value == s) location
        else null
    }
}

val Tokens.lastConsumedLocation: Location
    get() {
        val loc = this.lastConsumedLocation();
        return Location(loc.fromLine, loc.fromColumn)
    }
val Tokens.location: Location
    get() = Location(this.line, this.column)
