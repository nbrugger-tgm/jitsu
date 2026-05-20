import capabilities.graphDiagnostic
import helpers.joinAll
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.WorkspaceService
import workspace.WorkspaceDetector
import workspace.Workspaces
import java.util.concurrent.CompletableFuture

class JitsuWorkspaceService(
    val workspaces: Workspaces,
    val log: (Any) -> Unit,
    val client: () -> LanguageClient,
    val settings: JitsuLanguageServerSettings
) : WorkspaceService {

    override fun didChangeConfiguration(params: DidChangeConfigurationParams?) {
        log("didChangeConfiguration($params)")
    }

    override fun didChangeWatchedFiles(params: DidChangeWatchedFilesParams?) {
        params?.changes?.forEach { change ->
            val fileName = change.uri.split("/").last()
            val isGradleFile = change.uri.endsWith(".gradle") || change.uri.endsWith(".gradle.kts")
            val isModuleInfo = fileName.split(".").first() == "module-info" && fileName.split(".").last() == "json"
            val structuralFile = isGradleFile || isModuleInfo
            if (structuralFile) workspaces.workspaces
                .filter { change.uri.startsWith(it.uri.toString()) }
                .maxBy { it.uri.path.length }
                .reloadModules(client(), settings)
        }
        log("didChangeWatchedFiles($params)")
    }

    override fun symbol(params: WorkspaceSymbolParams?): CompletableFuture<Either<List<SymbolInformation?>?, List<WorkspaceSymbol?>?>?>? {
        log("symbol($params)")
        return null
    }

    override fun resolveWorkspaceSymbol(workspaceSymbol: WorkspaceSymbol?): CompletableFuture<WorkspaceSymbol?>? {
        log("resolveWorkspaceSymbol($workspaceSymbol)")
        return null
    }

    override fun didChangeWorkspaceFolders(params: DidChangeWorkspaceFoldersParams?) {
        log("didChangeWorkspaceFolders($params)")
        params?.event?.removed?.forEach { toRemove ->
            workspaces.remove(toRemove.uri)
        }
        WorkspaceDetector.loadWorkspaces(params?.event?.added ?: listOf(), client(), settings).thenApply {
            workspaces.addAll(it)
        }
    }

    override fun diagnostic(params: WorkspaceDiagnosticParams): CompletableFuture<WorkspaceDiagnosticReport> {
        log("workspace diagnostic($params)")
        val diagnosticId = workspaces.diagnosticsId
        val unchanged = params.previousResultIds.filter { it.value == diagnosticId }.map { it.uri }
        val required = params.previousResultIds.map { it.uri }
        return workspaces.workspaces.flatMap { it.modules }.map { it.graph }.joinAll().thenApply {
            WorkspaceDiagnosticReport(
                it.map { graphDiagnostic(it) }
                    .reduce { acc, map -> acc + map }
                    .let { map ->
                        val map = map.toMutableMap()
                        required.forEach {
                            map.putIfAbsent(it, listOf())
                        }
                        map
                    }
                    .map { (file, diagnostics) ->
                        if (unchanged.contains(file))
                            WorkspaceDocumentDiagnosticReport(WorkspaceUnchangedDocumentDiagnosticReport(diagnosticId, file, workspaces.getDocument(file).version))
                        else WorkspaceDocumentDiagnosticReport(WorkspaceFullDocumentDiagnosticReport(
                            diagnostics,
                            file,
                            workspaces.getDocument(file).version
                        ).also {
                            it.resultId = diagnosticId
                        })
                    }
            ).also { log("workspace diagnostic response: $it") }
        }
    }

    override fun didCreateFiles(params: CreateFilesParams?) {
        log("didCreateFiles($params)")
    }
}