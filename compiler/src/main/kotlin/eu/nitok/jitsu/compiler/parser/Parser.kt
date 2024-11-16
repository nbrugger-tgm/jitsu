package eu.nitok.jitsu.compiler.parser

import com.niton.jainparse.token.DefaultToken
import com.niton.jainparse.token.DefaultToken.*
import com.niton.jainparse.token.TokenSource
import com.niton.jainparse.token.TokenStream
import com.niton.jainparse.token.Tokenizer
import com.niton.jainparse.token.Tokenizer.AssignedToken
import eu.nitok.jitsu.compiler.ast.*
import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage
import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage.Hint
import eu.nitok.jitsu.compiler.parser.parsers.parseIdentifier
import eu.nitok.jitsu.compiler.parser.parsers.parseStatements
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


fun <T> Tokens.enclosedRepetition(
    start: DefaultToken,
    delimitter: DefaultToken,
    end: DefaultToken,
    messages: CompilerMessages,
    subject: String,
    elementName: String,
    function: (Tokens) -> T?
): List<T>? {
    val openKw = attempt(start)?.location ?: return null;
    val lst = mutableListOf<T>()
    skipWhitespace()
    if (attempt(end) != null)
        return lst
    while (hasNext()) {
        when (val x = function(this)) {
            null -> {
                skip(WHITESPACE)//dont skip line breaks
                val invalid = skipUntil(end, delimitter, NEW_LINE, SEMICOLON)
                messages.error(CompilerMessage("Expected a $elementName", invalid))
                if (peekOptional().getOrNull()?.type != delimitter) {
                    //nothing invalid was skipped - so end of block
                    break;
                }
                skip(delimitter)
            }

            else -> {
                lst.add(x)
                val postElemPos = location
                skipWhitespace()
                when (val delim = attempt(delimitter, end)?.value?.type) {
                    null -> messages.error("Expected $delimitter or $end", postElemPos.toRange())
                    end -> return lst
                    delimitter -> {
                        skipWhitespace()
                        continue
                    }
                    else -> throw IllegalStateException("$delim not a $end or $delimitter")
                }
            }
        }
        skipWhitespace()
    }

    skipWhitespace()

    this@enclosedRepetition.attempt(end) ?: messages.error(
        "Unclosed $subject, expected $end", location.toRange(), Hint(
            "$subject started here", openKw
        )
    )
    return lst
}

/**
 * Skips all kind of whitespace, including newlines and EOF (while EOF still terminates the stream)
 */
fun Tokens.skipWhitespace() {
    skip(WHITESPACE, NEW_LINE)
}

/**
 * Records the range of characters consumed during the `action` and returns the located result of parsing.
 */
inline fun <T> Tokens.range(action: ParserFn<T>): Located<T> {
    val start = location
    val res = action()
    val end = lastConsumedLocation
    return Located(res, start.rangeTo(end))
}

/**
 * Records the range of characters consumed during the `action` and returns the located result of parsing simmilar to [range].
 * The difference is how nullability is handled. If the result of [action] is null then the range won't be returned since that would be nonsensical.
 */
inline fun <T> Tokens.nullableRange(action: ParserFn<T?>): Located<T>? {
    val start = location
    val res = action()
    if (res == null) return null
    val end = lastConsumedLocation
    return Located(res, start.rangeTo(end))
}

/**
 * Expects any of the given tokens, if the next token is one of them it records it and returns otherwise returns null on non match
 *
 * Only proceeds and progresses the stream on match
 */
fun Tokens.attempt(vararg tokens: DefaultToken): Located<AssignedToken<DefaultToken>>? {
    val next = peekOptional().getOrNull() ?: return null;
    if (tokens.contains(next.type)) {
        return range { this.next() }
    }
    return null;
}

/**
 * handles a transaction by automatically rolling back and committing based on if parsing is successfull
 * @param function a function that performs parsing and returns null when unparsable, returning null causes a rollback
 */
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


/**
 * @return The range of the keyword, or null if the keyword is not present
 */
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