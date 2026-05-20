package workspace

import JitsuLanguageServerSettings
import org.eclipse.lsp4j.services.LanguageClient
import java.net.URI

class Workspace(
    val uri: URI,
    val modules: MutableList<WorkspaceModule>
) {
    var version = 0;
    fun reloadModules(client: LanguageClient, settings: JitsuLanguageServerSettings) {
        WorkspaceDetector.loadWorkspace(
            uri = uri,
            client = client,
            settings = settings
        ).thenAccept {
            modules.clear()
            modules.addAll(it.modules)
            version++
        }
    }
}