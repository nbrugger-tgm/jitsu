package capabilities

import org.eclipse.lsp4j.DiagnosticRegistrationOptions
import org.eclipse.lsp4j.SemanticTokensLegend
import org.eclipse.lsp4j.SemanticTokensWithRegistrationOptions
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.TextDocumentSyncKind
import org.eclipse.lsp4j.WorkspaceFoldersOptions
import org.eclipse.lsp4j.WorkspaceServerCapabilities
import org.eclipse.lsp4j.jsonrpc.messages.Either

val capabilities = ServerCapabilities().also {
    it.experimental = false;
    it.semanticTokensProvider = SemanticTokensWithRegistrationOptions().also { semanticTokens ->
        semanticTokens.full = Either.forLeft(true);
        semanticTokens.legend = SemanticTokensLegend(
            SemanticTokenTypes.entries.sortedBy { it.ordinal }.map { it.id },
            SemanticTokenModifiers.entries.sortedBy { it.ordinal }.map { it.id },
        )
    }

    it.colorProvider = Either.forLeft(true)
    it.documentSymbolProvider = Either.forLeft(true)
//        it.completionProvider = CompletionOptions()
//        it.completionProvider.completionItem = CompletionItemOptions()
//        it.completionProvider.completionItem.labelDetailsSupport = true;
//        it.completionProvider.resolveProvider = true;
    it.documentHighlightProvider = Either.forLeft(true);
    it.textDocumentSync = Either.forLeft(TextDocumentSyncKind.Full);
    it.diagnosticProvider = DiagnosticRegistrationOptions().also { diagnostics ->
        diagnostics.isInterFileDependencies = true;
        diagnostics.isWorkspaceDiagnostics = true;
    }
    it.definitionProvider = Either.forLeft(true)
    it.referencesProvider = Either.forLeft(true)

    it.workspace = WorkspaceServerCapabilities().also {
        it.workspaceFolders = WorkspaceFoldersOptions()
        it.workspaceFolders.supported = true
        it.workspaceFolders.changeNotifications = Either.forRight(true)
    }
    it.workspaceSymbolProvider = Either.forLeft(true)
}