package capabilities

import eu.nitok.jitsu.compiler.ast.AstNode
import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage
import eu.nitok.jitsu.compiler.graph.JitsuFile
import eu.nitok.jitsu.compiler.graph.Scope
import eu.nitok.jitsu.compiler.model.flatMap
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import range

fun syntaxDiagnostic(node: AstNode): List<Diagnostic> {
    return node.flatMap {
        val warningDiagnostics = it.warnings.map { warnDiagnostic(it) }
        val errorDiagnostics = it.errors.map { errorDiagnostic(it) }
        return@flatMap warningDiagnostics + errorDiagnostics;
    }
}
fun syntaxDiagnostic(scope: JitsuFile): List<Diagnostic> {
    return scope.messages.errors.map { errorDiagnostic(it) } + scope.messages.warnings.map { warnDiagnostic(it) }
}

private fun errorDiagnostic(err: CompilerMessage): Diagnostic {
    return Diagnostic(
        range(err.location.toRange()),
        err.message,
        DiagnosticSeverity.Error,
        "jitsu"
    )
}
private fun warnDiagnostic(err: CompilerMessage): Diagnostic {
    return Diagnostic(
        range(err.location.toRange()),
        err.message,
        DiagnosticSeverity.Warning,
        "jitsu"
    )
}