import capabilities.SemanticTokenModifiers
import capabilities.SemanticTokenTypes
import eu.nitok.jitsu.compiler.JitsuCompilerInfo
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.*
import java.util.concurrent.CompletableFuture

class JitsuLanguageServer : LanguageServer, LanguageClientAware{
    override fun initialize(params: InitializeParams?): CompletableFuture<InitializeResult> {
        if(params == null) CompletableFuture.failedFuture<InitializeResult>(NullPointerException("required params"))
        val result = InitializeResult()
        result.serverInfo = ServerInfo()
        result.serverInfo.name = JitsuCompilerInfo.name
        result.serverInfo.version = JitsuCompilerInfo.version

        result.capabilities = ServerCapabilities()
        result.capabilities.experimental = false;
        result.capabilities.semanticTokensProvider = SemanticTokensWithRegistrationOptions();
        result.capabilities.semanticTokensProvider.full = Either.forLeft(true);
        result.capabilities.semanticTokensProvider.legend = SemanticTokensLegend(
            SemanticTokenTypes.entries.sortedBy { it.ordinal }.map { it.id },
            SemanticTokenModifiers.entries.sortedBy { it.ordinal }.map { it.id },
        )
        result.capabilities.colorProvider = Either.forLeft(true);
        result.capabilities.documentSymbolProvider = Either.forLeft(true);
//        result.capabilities.completionProvider = CompletionOptions()
//        result.capabilities.completionProvider.completionItem = CompletionItemOptions()
//        result.capabilities.completionProvider.completionItem.labelDetailsSupport = true;
//        result.capabilities.completionProvider.resolveProvider = true;
        result.capabilities.documentHighlightProvider = Either.forLeft(true);
        result.capabilities.textDocumentSync = Either.forLeft(TextDocumentSyncKind.Full);
        result.capabilities.diagnosticProvider = DiagnosticRegistrationOptions();
        result.capabilities.diagnosticProvider.isInterFileDependencies = true;
        result.capabilities.diagnosticProvider.isWorkspaceDiagnostics = false;
        return CompletableFuture.completedFuture(result)
    }


    override fun setTrace(params: SetTraceParams?) {
    }
    override fun shutdown(): CompletableFuture<Any> {
        return CompletableFuture.completedFuture("success")
    }

    override fun exit() {
    }

    override fun getTextDocumentService(): TextDocumentService {
        return JitsuFileService(this)
    }

    override fun getWorkspaceService(): WorkspaceService {
        return JitsuWorkspaceService()
    }

    var customLogger: java.io.PrintWriter? = null;
    internal var client: LanguageClient? = null

    override fun connect(client: LanguageClient?) {
        this.client = client;
    }
}