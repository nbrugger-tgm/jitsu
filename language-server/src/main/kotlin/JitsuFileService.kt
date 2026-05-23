import capabilities.documentSymbols
import capabilities.findDefinition
import capabilities.graphDiagnostic
import capabilities.syntaxDiagnostic
import capabilities.syntaxHighlight
import eu.nitok.jitsu.common.locating.Position
import eu.nitok.jitsu.common.sequence
import eu.nitok.jitsu.compiler.graph.api.Accessible
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.TextDocumentService
import workspace.Workspaces
import java.util.concurrent.CompletableFuture

class JitsuFileService(val workspaces: Workspaces, val log: (Any) -> Unit) : TextDocumentService {

    override fun didOpen(params: DidOpenTextDocumentParams) {
        log("didOpen($params)")
        workspaces.getDocument(params.textDocument.uri).setContent(params.textDocument.text)
    }

    override fun didChange(params: DidChangeTextDocumentParams) {
        val document = workspaces.getDocument(params.textDocument.uri)
        if (params.contentChanges.size == 1 && params.contentChanges[0].range == null) {
            log("Full text sync: ${document.uri}")
            document.setContent(params.contentChanges[0].text)
            return
        }
        TODO("incremental sync not supported")
    }

    override fun didClose(params: DidCloseTextDocumentParams) {
    }

    override fun didSave(params: DidSaveTextDocumentParams) {
    }

    override fun documentHighlight(params: DocumentHighlightParams?): CompletableFuture<MutableList<out DocumentHighlight>> {
        //highlight related tokens (usage of variable, function, etc), matching brackets, etc
        return CompletableFuture.completedFuture(mutableListOf())
    }


    override fun documentSymbol(params: DocumentSymbolParams): CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> {
        val graph = workspaces.getDocument(params.textDocument.uri).graph ?: return CompletableFuture.completedFuture(
            mutableListOf()
        );


        return graph.thenApply {
            it.documentSymbols().map<DocumentSymbol, Either<SymbolInformation, DocumentSymbol>> {
                Either.forRight(it)
            }
        }
    }


    override fun semanticTokensFull(params: SemanticTokensParams): CompletableFuture<SemanticTokens> {
        val ast = workspaces.getDocument(params.textDocument.uri).ast

        return ast.thenApply {
            SemanticTokens(syntaxHighlight(it))
        }
    }

    private fun color(it: Position, write: DocumentHighlightKind): ColorInformation {
        return ColorInformation(
            range(it.rangeTo(it)),
            Color(1.0, .0, .0, 1.0)
        )
    }

    override fun definition(params: DefinitionParams): CompletableFuture<Either<MutableList<out Location>, MutableList<out LocationLink>>> {
        val referencePos = location(params.position, params.textDocument.uri)
        val document = workspaces.getDocument(params.textDocument.uri).graph
        return document.thenApply { it.findDefinition(referencePos) }
    }

    override fun references(params: ReferenceParams): CompletableFuture<List<Location?>?>? {
        val definitionPos = location(params.position, params.textDocument.uri)
        val graph = workspaces.getDocument(params.textDocument.uri).graph
        val definition = graph.thenApply {
            it.sequence()
                .filterIsInstance<Accessible<*>>()
                .find { it.name?.location?.contains(definitionPos) ?: false }
        }
        return definition.thenApply {
            it?.accessToSelf?.map {
                it.reference.location.let { location(it) }
            } ?: emptyList()
        }
    }

    override fun diagnostic(params: DocumentDiagnosticParams): CompletableFuture<DocumentDiagnosticReport> {
        log("file diagnostic($params)")
        val document = workspaces.getDocument(params.textDocument.uri)
        val ast = document.ast
        val graph = document.graph
        return ast.thenCombine(graph) { ast, graph ->
            DocumentDiagnosticReport(
                RelatedFullDocumentDiagnosticReport(
                    syntaxDiagnostic(ast) + graphDiagnostic(graph)
                ).also {
                    it.resultId = workspaces.diagnosticsId.toString()
                }
            ).also { log("file diagnostic response: $it") }
        }
    }

    override fun colorPresentation(params: ColorPresentationParams?): CompletableFuture<MutableList<ColorPresentation>> {
        return CompletableFuture.completedFuture(mutableListOf())
    }

    override fun documentColor(params: DocumentColorParams?): CompletableFuture<MutableList<ColorInformation>> {
        return CompletableFuture.completedFuture(mutableListOf())//css like color editor
    }
}