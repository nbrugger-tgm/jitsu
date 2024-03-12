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
        next()
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
    val openKw = expect(start)?.location ?: return null;
    val lst = mutableListOf<T>()

    while (hasNext()) {
        skipWhitespace();
        when (val x = function(this)) {
            null -> {
                skip(WHITESPACE)//dont skip line breaks
                index()
                val invalid = skipUntil(end, delimitter, NEW_LINE, SEMICOLON)
                messages.error(CompilerMessage("Expected a $elementName", invalid))
                if (peek().type != delimitter) {
                    //nothing invalid was skipped - so end of block
                    break;
                }
                skip(delimitter)
            }

            else -> {
                lst.add(x)
                val postElemPos = location;
                skipWhitespace()
                when (val delim = expect(delimitter, end)?.value?.type) {
                    null -> messages.error("Expected $delimitter or $end", postElemPos.toRange())
                    end -> return lst
                    delimitter -> continue
                    else -> throw IllegalStateException("$delim not a $end or $delimitter")
                }
            };
        }
    }

    skipWhitespace()

    expect(end) ?: messages.error(
        "Unclosed $subject, expected $end", location.toRange(), Hint(
            "$subject started here", openKw
        )
    )
    return lst
}


fun Tokens.skipWhitespace() {
    skip(WHITESPACE, NEW_LINE)
}

fun Tokens.keyword(s: DefaultToken): Range? {
    elevate()
    if (!hasNext()) return null;
    val token = range { next() }
    return if (token.value.type == s) {
        commit()
        token.location
    } else {
        rollback()
        null
    }
}


/**
 * @return The range of the keyword, or null if the keyword is not present
 */
fun Tokens.keyword(s: String): Range? {
    elevate()
    val (token, location) = range { nextOptional().getOrNull() }
    token ?: return null
    return if (token.value == s) {
        commit()
        location
    } else {
        rollback()
        null
    }
}

inline fun <T> Tokens.range(action: Tokens.() -> T): Located<T> {
    val start = location
    val res = action()
    val end = lastConsumedLocation
    return Located(res, start.rangeTo(end))
}

fun Tokens.expect(vararg tokens: DefaultToken): Located<AssignedToken<DefaultToken>>? {
    val next = peekOptional().getOrNull() ?: return null;
    if (tokens.contains(next.type)) {
        return range { next() }
    }
    return null;
}

val Tokens.lastConsumedLocation: Location
    get() {
        val loc = this.lastConsumedLocation();
        return Location(loc.fromLine, loc.fromColumn)
    }
val Tokens.location: Location
    get() = Location(this.line, this.column)