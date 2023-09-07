import capabilities.documentSymbols
import capabilities.syntaxHighlight
import com.niton.parser.ast.LocatableReducedNode
import com.niton.parser.ast.ParsingResult
import eu.nitok.jitsu.compiler.ast.Location
import eu.nitok.jitsu.compiler.ast.StatementNode
import eu.nitok.jitsu.compiler.ast.buildFileAst
import eu.nitok.jitsu.compiler.graph.Scope
import eu.nitok.jitsu.compiler.graph.buildGraph
import eu.nitok.jitsu.compiler.parser.jainparse.parseFile
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.TextDocumentService
import java.io.File
import java.net.URI
import java.util.concurrent.CompletableFuture

class JitsuFileService(val server: JitsuLanguageServer) : TextDocumentService {
    private val rawTexts: MutableMap<String, String> = mutableMapOf();
    private val rawAsts = mutableMapOf<String, ParsingResult<LocatableReducedNode>?>()
    private val asts = mutableMapOf<String, Lazy<List<StatementNode>?>>()
    private val graphs = mutableMapOf<String, Lazy<Scope?>>()
    override fun didOpen(params: DidOpenTextDocumentParams?) {
        params?.textDocument?.let {
            rawTexts[it.uri] = it.text
            updateFile(it.uri)
        }
    }

    private fun updateFile(uri: String) {
        val text = rawTexts[uri] ?: return
        rawAsts[uri] = run {
            val ast = parseFile(text)
//            server.client?.publishDiagnostics(
//                PublishDiagnosticsParams(
//                    uri, syntaxDiagnostic(ast)
//                )
//            )
            return@run ast
        }
        val oldAst = asts[uri]?.value;
        asts[uri] = lazy {
            val result = rawAsts[uri]
            return@lazy if (result?.wasParsed() == true) {
                buildFileAst(result.unwrap())
            } else oldAst
        }
        graphs[uri] = lazy { asts[uri]?.value?.let { buildGraph(it) } }
    }

    override fun didChange(params: DidChangeTextDocumentParams?) {
        val url = params?.textDocument?.uri ?: return
        if(params.contentChanges.size == 1){
            rawTexts[url] = params.contentChanges[0].text
        }else {
            val oldText = (rawTexts[url]?.split("\n") ?: return).toMutableList()
            val shift : MutableMap<Int, Int> = mutableMapOf();
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
        val text = params?.textDocument?.uri?.let  {
            val file = File(URI(it))
            return@let file.readText();
        }
        rawTexts[params?.textDocument?.uri ?: return] = text ?: return
        updateFile(params.textDocument.uri)
    }

    override fun didSave(params: DidSaveTextDocumentParams?) {
        val text = params?.textDocument?.uri?.let  {
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

        val flatMap = ast.flatMap { it.documentSymbols() }
        val either = flatMap.map<DocumentSymbol, Either<SymbolInformation, DocumentSymbol>> {
            Either.forRight(it)
        }
        return CompletableFuture.completedFuture(either.toMutableList())
    }


    override fun semanticTokensFull(params: SemanticTokensParams?): CompletableFuture<SemanticTokens> {
        val ast = asts[params?.textDocument?.uri]?.value ?:// return CompletableFuture.failedFuture(NullPointerException("no ast"))
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
        return CompletableFuture.completedFuture(DocumentDiagnosticReport(RelatedFullDocumentDiagnosticReport(
            params?.textDocument?.uri?.let { rawAsts[it] }?.let { syntaxDiagnostic(it) } ?: listOf()
        )))
    }

    private fun syntaxDiagnostic(rawAst: ParsingResult<LocatableReducedNode>): List<Diagnostic> {
        return if (!rawAst.wasParsed()) {
            val err = rawAst.exception().mostProminentDeepException
            listOf(
                Diagnostic(
                    range(err.location.get()),
                    err.message ?: "syntax error",
                    DiagnosticSeverity.Error,
                    "jainparse"
                )
            )
        } else {
            listOf()
        }
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

    private fun color(it: StatementNode): List<ColorInformation> {
        return when (it) {
            is StatementNode.AssignmentNode -> listOf(color(it.location, DocumentHighlightKind.Write))
            is StatementNode.CodeBlockNode.SingleExpressionCodeBlock -> listOf()
            is StatementNode.CodeBlockNode.StatementsCodeBlock -> listOf()
//            is StatementNode.EnumDeclarationNode -> listOf()
            is StatementNode.FunctionCallNode -> listOf()
            is StatementNode.FunctionDeclarationNode -> listOf()
            is StatementNode.IfNode -> listOf()
            is StatementNode.MethodInvocationNode -> listOf()
            is StatementNode.ReturnNode -> listOf()
            is StatementNode.SwitchNode -> listOf()
            is StatementNode.TypeDefinitionNode -> listOf()
            is StatementNode.VariableDeclarationNode -> listOf(color(it.location, DocumentHighlightKind.Write))
        }
    }
}

internal fun range(it: Location) = Range(Position(it.fromLine-1, it.fromColumn-1), Position(it.toLine-1, it.toColumn-1))
private var i: Long = 0;
internal fun getArtificalId(): Long {
    return i++;
}