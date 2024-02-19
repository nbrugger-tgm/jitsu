import capabilities.documentSymbols
import capabilities.syntaxDiagnostic
import capabilities.syntaxHighlight
import com.niton.jainparse.token.ListTokenStream
import com.niton.jainparse.token.Tokenizer

import eu.nitok.jitsu.compiler.ast.AstNode
import eu.nitok.jitsu.compiler.ast.StatementNode
import eu.nitok.jitsu.compiler.graph.Scope
import eu.nitok.jitsu.compiler.graph.buildGraph
import eu.nitok.jitsu.compiler.parser.Parser
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.TextDocumentService
import java.io.File
import java.net.URI
import java.util.concurrent.CompletableFuture

class JitsuFileService(val server: JitsuLanguageServer) : TextDocumentService {
    private val rawTexts: MutableMap<String, String> = mutableMapOf();
    private val asts = mutableMapOf<String, Lazy<List<AstNode<StatementNode>>>>()
    private val graphs = mutableMapOf<String, Lazy<Scope?>>()
    private val tokenizer: Tokenizer = Tokenizer()
    override fun didOpen(params: DidOpenTextDocumentParams?) {
        params?.textDocument?.let {
            rawTexts[it.uri] = it.text
            updateFile(it.uri)
        }
    }

    private fun updateFile(uri: String) {
        val text = rawTexts[uri] ?: return
        asts[uri] = lazy {
            customLogger.println("parsing $uri")
            val startMs = System.currentTimeMillis()
            val tokens = tokenizer.tokenize(text).unwrap()
            val ast = Parser.file(ListTokenStream(tokens))
            val endMs = System.currentTimeMillis()
            val parsingTime = endMs - startMs
            customLogger.println("parsed in ${parsingTime}ms")
            customLogger.println("Per char: ${parsingTime / text.length}ms")
            customLogger.println("Per line: ${parsingTime / text.split("\n").size}ms")
            customLogger.println("Per token: ${parsingTime / tokens.size}ms")
            graphs[uri] = lazy {
                customLogger.println("build graph for $uri")
                buildGraph(ast)
            }
            server.client?.refreshDiagnostics()
            server.client?.refreshSemanticTokens()
            return@lazy ast
        }
        graphs[uri] = lazy { asts[uri]?.value?.let { buildGraph(it) } }
    }

    override fun didChange(params: DidChangeTextDocumentParams?) {
        val url = params?.textDocument?.uri ?: return
        if (params.contentChanges.size == 1) {
            rawTexts[url] = params.contentChanges[0].text
        } else {
            val oldText = (rawTexts[url]?.split("\n") ?: return).toMutableList()
            val shift: MutableMap<Int, Int> = mutableMapOf();
            params.contentChanges.forEach() {
                val lineIndex = it.range.start.line
                if (lineIndex != it.range.end.line) throw UnsupportedOperationException("multiline changes are not supported")
                val oldLen = it.range.end.character - it.range.start.character
                var line = oldText[lineIndex]
                val thisShift = shift[lineIndex] ?: 0;
                line = line.substring(
                    0,
                    it.range.start.character + thisShift
                ) + it.text + line.substring(it.range.end.character + thisShift)
                oldText[lineIndex] = line
                val delta = it.text.length - oldLen
                shift[lineIndex] = (shift[lineIndex] ?: 0) + delta;
            }
            rawTexts[url] = oldText.joinToString("\n")
            customLogger.println("changed to ${rawTexts[url]}")
        }
        updateFile(url)
    }

    override fun didClose(params: DidCloseTextDocumentParams?) {
        val text = params?.textDocument?.uri?.let {
            val file = File(URI(it))
            return@let file.readText();
        }
        rawTexts[params?.textDocument?.uri ?: return] = text ?: return
        updateFile(params.textDocument.uri)
    }

    override fun didSave(params: DidSaveTextDocumentParams?) {
        val text = params?.textDocument?.uri?.let {
            val file = File(URI(it))
            return@let file.readText();
        }
        rawTexts[params?.textDocument?.uri ?: return] = text ?: return
        updateFile(params.textDocument.uri)
    }

    override fun documentHighlight(params: DocumentHighlightParams?): CompletableFuture<MutableList<out DocumentHighlight>> {
        //highlight related tokens (usage of variable, function, etc), matching brackets, etc
        return CompletableFuture.completedFuture(mutableListOf())
    }


    override fun documentSymbol(params: DocumentSymbolParams?): CompletableFuture<MutableList<Either<SymbolInformation, DocumentSymbol>>> {
        val ast = asts[params?.textDocument?.uri]?.value ?: return CompletableFuture.completedFuture(mutableListOf());

        val flatMap = ast.flatMap {
            when (it) {
                is AstNode.Node -> it.value.documentSymbols()
                is AstNode.Error<*> -> listOf()
            }
        }
        val either = flatMap.map<DocumentSymbol, Either<SymbolInformation, DocumentSymbol>> {
            Either.forRight(it)
        }
        return CompletableFuture.completedFuture(either.toMutableList())
    }


    override fun semanticTokensFull(params: SemanticTokensParams?): CompletableFuture<SemanticTokens> {
        val ast = asts[params?.textDocument?.uri]?.value
            ?:// return CompletableFuture.failedFuture(NullPointerException("no ast"))
            return CompletableFuture.completedFuture(SemanticTokens(listOf()))
        val tokens = syntaxHighlight(ast)
        customLogger.println("tokens: $tokens")
        customLogger.flush()
        return CompletableFuture.completedFuture(SemanticTokens(tokens))//syntax highlighting
    }

    private fun color(it: Location, write: DocumentHighlightKind): ColorInformation {
        return ColorInformation(
            range(it),
            Color(1.0, .0, .0, 1.0)
        )
    }

    override fun diagnostic(params: DocumentDiagnosticParams?): CompletableFuture<DocumentDiagnosticReport> {
        val ast = params?.textDocument?.uri?.let { asts[it] }?.value
        return CompletableFuture.completedFuture(DocumentDiagnosticReport(RelatedFullDocumentDiagnosticReport(
            ast?.flatMap { syntaxDiagnostic(it) } ?: listOf()
        )))
    }

    override fun colorPresentation(params: ColorPresentationParams?): CompletableFuture<MutableList<ColorPresentation>> {
        return CompletableFuture.completedFuture(
            mutableListOf(
                ColorPresentation(
                    "lol2",
                    TextEdit(Range(Position(0, 0), Position(0, 0)), "lol")
                )
            )
        )
    }

    override fun documentColor(params: DocumentColorParams?): CompletableFuture<MutableList<ColorInformation>> {
        return CompletableFuture.completedFuture(mutableListOf())//css like color editor
    }
}

internal fun range(it: Location) =
    Range(Position(it.fromLine - 1, it.fromColumn - 1), Position(it.toLine - 1, it.toColumn - 1))

private var i: Long = 0;
internal fun getArtificalId(): Long {
    return i++;
}