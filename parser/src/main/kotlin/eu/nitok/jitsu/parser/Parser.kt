package eu.nitok.jitsu.parser

import com.niton.jainparse.token.DefaultToken
import com.niton.jainparse.token.DefaultToken.*
import com.niton.jainparse.token.TokenSource
import com.niton.jainparse.token.TokenStream
import com.niton.jainparse.token.Tokenizer
import com.niton.jainparse.token.Tokenizer.AssignedToken
import eu.nitok.jitsu.common.CompilerMessage
import eu.nitok.jitsu.common.CompilerMessage.Hint
import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.common.locating.Location
import eu.nitok.jitsu.common.locating.Position
import eu.nitok.jitsu.parser.ast.*
import eu.nitok.jitsu.parser.parsers.parseIdentifier
import eu.nitok.jitsu.parser.parsers.parseStatements
import eu.nitok.jitsu.parser.tokenization.FileTokenStream
import java.io.StringReader
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.jvm.optionals.getOrNull
import kotlin.streams.asSequence

typealias Tokens = FileTokenStream
typealias ParserFn<T> = Tokens.() -> T

fun parseJitsuModule(module: Path, name: String?): JitsuModuleAst {
    if (!module.exists()) throw IllegalArgumentException("module directory does not exist $module")
    if (module.isRegularFile()) throw IllegalArgumentException("expect a source directory, not file $module")

    val sources = Files.list(module).asSequence().filter { it.isRegularFile() && it.name.endsWith(".jit") }
    val subModules = Files.list(module).asSequence().filter { it.isDirectory() }
    val sourceAsts = sources.map { parseJitsuFile(it.readText(), it.toUri()) }.toList()
    return JitsuModuleAst(name ?: module.name, sourceAsts, subModules.map { parseJitsuModule(it, null) }.toList())
}

fun parseJitsuModule(moduleFiles: Set<Path>, name: String): JitsuModuleAst {
    val commonRoot = moduleFiles.reduce { acc, paths ->
        var common = acc
        while (!paths.startsWith(common)) {
            common = acc.parent
        }
        common
    }

    val filesByModule = moduleFiles.groupBy({ it.parent }) { parseJitsuFile(it.readText(), it.toUri()) }

    fun findSubModules(path: Path): List<JitsuModuleAst> {
        val subModulesWithFiles = filesByModule.entries.asSequence()
            .filter { it.key.parent.isSameFileAs(path) }
            .map { JitsuModuleAst(it.key.name, it.value, findSubModules(it.key)) }
            .toList()
        val subModulesWithoutFiles = filesByModule.entries.asSequence()
            .filter { it.key.parent.startsWith(path) }
            .map {
                var emptyModule = it.key
                while(!it.key.parent.isSameFileAs(path)) {
                    emptyModule = it.key.parent
                }
                emptyModule
            }
            .filter { filesByModule[it]?.isEmpty()?: true }
            .map { JitsuModuleAst(it.name, emptyList(), findSubModules(it)) }
            .toList()
        return subModulesWithFiles + subModulesWithoutFiles
    }

    return JitsuModuleAst(name, filesByModule[commonRoot]?:listOf(), findSubModules(commonRoot))
}

fun parseJitsuFile(txt: String, uri: URI): SourceFileNode {
    val tokenSource = TokenSource(StringReader(txt), tokenizer);
    val tokens = FileTokenStream(uri, TokenStream.of(tokenSource))
    val statements = mutableListOf<StatementNode>()
    val sourceFileNode = SourceFileNode(uri, statements)
    parseStatements(tokens, statements, sourceFileNode::error)
    return sourceFileNode;
}

internal fun Tokens.skipUntil(vararg stoppers: DefaultToken): Location {
    return range {
        while (peekOptional().map { !stoppers.contains(it.type) }.orElse(false)) skip()
    }.location
}

internal fun Tokens.skip(vararg toSkip: DefaultToken) {
    while (this.hasNext() && toSkip.contains(this.peek().type)) {
        this.next()
    }
}

internal fun Tokens.skip(n: Int = 1) {
    var count = n;
    while (count-- > 0 && hasNext()) {
        this@skip.next()
    }
}

private val tokenizer = Tokenizer(DefaultToken.entries)

internal fun <T> parseIdentifierBased(tokens: Tokens, parser: (tokens: Tokens, id: IdentifierNode) -> T?): T? {
    tokens.elevate()
    val id = parseIdentifier(tokens) ?: run {
        tokens.rollback()
        return null
    }
    val res = parser(tokens, id)
    if (res == null) tokens.rollback()
    else tokens.commit()
    return res
}

internal sealed interface LastElementState {
    object Delimitted : LastElementState
    data class NotDelimitted(val position: Position) : LastElementState
}

internal data class EnclosedRepetition<T>(val openKw: Location, val elements: List<T>, val closeKw: Location?)

internal fun <T> Tokens.enclosedRepetition(
    start: DefaultToken,
    delimitter: DefaultToken,
    end: DefaultToken,
    messages: CompilerMessages,
    subject: String,
    elementName: String,
    invalidObjectPlaceholder: T? = null,
    parseElement: (Tokens) -> T?
): EnclosedRepetition<T>? {
    val openKw = attempt(start)?.location ?: return null;

    skipWhitespace()
    var endKw = attempt(end)?.location
    if (endKw != null) return EnclosedRepetition(openKw, listOf(), endKw)

    val lst = mutableListOf<T>()
    var wasLastElementDelimitted: LastElementState = LastElementState.Delimitted
    while (hasNext()) {
        when (val x = parseElement(this)) {
            null -> {
                if (wasLastElementDelimitted is LastElementState.NotDelimitted) {
                    //The last element was not followed by a delimitter, therefore an invalid element here means
                    // Its more probable that the user forgot the "end" mark rather than that they forgot a delimitter
                    break
                }
                skip(WHITESPACE)
                val invalid = skipUntil(end, delimitter, NEW_LINE, SEMICOLON)
                messages.error(CompilerMessage("Expected a $elementName", invalid))
                val next = peekOptional().getOrNull()?.type
                if (invalidObjectPlaceholder != null && (next == end || next == delimitter))
                    lst.add(invalidObjectPlaceholder)
                if (next != delimitter) {
                    break;
                }
                skip(1)
                wasLastElementDelimitted = LastElementState.Delimitted
                skipWhitespace()
            }

            else -> {
                if (wasLastElementDelimitted is LastElementState.NotDelimitted) {
                    messages.error(
                        "Expected a '$delimitter' between $elementName elements",
                        wasLastElementDelimitted.position.toLocation()
                    )
                }
                lst.add(x)
                val expectedDelimiterPos = position
                skipWhitespace()
                val next = peekOptional().getOrNull()?.type
                wasLastElementDelimitted = when (next) {
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

    endKw = this@enclosedRepetition.attempt(end)?.location;
    endKw ?: messages.error(
        "Unclosed $subject, expected $end", position.toLocation(), Hint(
            "$subject started here", openKw
        )
    )
    return EnclosedRepetition(openKw, lst, endKw)
}

internal fun Tokens.skipWhitespace(): Tokens {
    skip(WHITESPACE, NEW_LINE)
    return this
}

internal inline fun <T> Tokens.range(action: ParserFn<T>): Located<T> {
    val start = position
    val res = action()
    val end = lastConsumedLocation
    return Located(res, start.rangeTo(end))
}

internal inline fun <T> Tokens.nullableRange(action: ParserFn<T?>): Located<T>? {
    val start = position
    val res = action()
    if (res == null) return null
    val end = lastConsumedLocation
    return Located(res, start.rangeTo(end))
}

internal fun Tokens.attempt(vararg tokens: DefaultToken): Located<AssignedToken<DefaultToken>>? {
    val next = peekOptional().getOrNull() ?: return null;
    if (tokens.contains(next.type)) {
        return range { this.next() }
    }
    return null;
}

internal fun <T> Tokens.attempt(function: ParserFn<T>): T? {
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

internal fun Tokens.keyword(s: String): Location? {
    return attempt {
        val (token, location) = range { nextOptional().getOrNull() }
        return@attempt if (token == null) null
        else if (token.value == s) location
        else null
    }
}

internal val Tokens.lastConsumedLocation: Position
    get() {
        val loc = this.lastConsumedLocation();
        return Position(loc.fromLine, loc.fromColumn, file)
    }
internal val Tokens.position: Position
    get() = Position(this.line, this.column, file)
