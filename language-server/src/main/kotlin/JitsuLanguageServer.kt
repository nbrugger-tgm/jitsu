import capabilities.capabilities
import eu.nitok.jitsu.compiler.JitsuCompilerInfo
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode
import org.eclipse.lsp4j.services.*
import workspace.WorkspaceDetector
import workspace.Workspaces
import java.io.PrintStream
import java.util.concurrent.CompletableFuture

class JitsuLanguageServer(private val log: PrintStream, val clientId: Long, val exitHook: () -> Unit) : LanguageServer,
    LanguageClientAware {

    fun log(text: Any) {
        log.println("[client/$clientId]: $text")
    }

    internal var client: LanguageClient? = null
    val settings = JitsuLanguageServerSettings.loadSystemSettings()
    val session = run {
        val workspaces = Workspaces()
        Session(
            fileService = JitsuFileService(workspaces, this::log),
            workspaceService = JitsuWorkspaceService(workspaces, this::log, { client!! }, settings),
        )
    }


    override fun initialize(params: InitializeParams?): CompletableFuture<InitializeResult> {
        if (params == null) CompletableFuture.failedFuture<InitializeResult>(NullPointerException("required params"))

        val result = InitializeResult()
        result.serverInfo = ServerInfo()
        result.serverInfo.name = JitsuCompilerInfo.name
        result.serverInfo.version = JitsuCompilerInfo.version

        result.capabilities = capabilities
        session.initialized = true

        return WorkspaceDetector.loadWorkspaces(params?.workspaceFolders?:listOf(), client!!, settings).thenAccept {
            session.workspaceService.workspaces.addAll(it)
        }.thenApply {
            result
        }
    }

    override fun initialized(params: InitializedParams?) {
        client?.registerCapability(
            RegistrationParams(
                listOf(
            Registration().also {
                it.id = "watcher"
                it.method = "workspace/didChangeWatchedFiles"
                it.registerOptions = mapOf(
                    "watchers" to listOf(
                        mapOf(
                            "globPattern" to "**/{*.jit,module-info*(.*).json,*.gradle,*.gradle.kts}"
                        )
                    )
                )
            }
        )))
    }

    override fun setTrace(params: SetTraceParams?) {
        log("setTrace ${params.toString()}")
    }

    override fun shutdown(): CompletableFuture<Any> {
        if (!session.initialized) return CompletableFuture.failedFuture(
            ResponseErrorException(
                ResponseError(
                    ResponseErrorCode.InvalidRequest,
                    "the server was already shutdown or isn't initialized yet",
                    null
                )
            )
        )
        session.initialized = false
        return CompletableFuture.completedFuture(null)
    }

    override fun exit() {
        log("Received EXIT request")
        exitHook()
    }

    override fun getTextDocumentService(): TextDocumentService {
        return session.fileService
    }

    override fun getWorkspaceService(): WorkspaceService {
        return session.workspaceService
    }


    override fun connect(client: LanguageClient?) {
        this.client = client;
    }
}