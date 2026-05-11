package capabilities

import eu.nitok.jitsu.parser.ast.AstNode
import eu.nitok.jitsu.common.CompilerMessage
import eu.nitok.jitsu.compiler.graph.JitsuModule
import eu.nitok.jitsu.common.flatMap
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
fun syntaxDiagnostic(scope: JitsuModule): List<Diagnostic> {
    return scope.messages.errors.map { errorDiagnostic(it) } +
            scope.messages.warnings.map { warnDiagnostic(it) }
}

private fun errorDiagnostic(err: CompilerMessage): Diagnostic {
    return Diagnostic(
        range(err.location.toLocation()),
        err.message,
        DiagnosticSeverity.Error,
        "jitsu"
    )
}
private fun warnDiagnostic(err: CompilerMessage): Diagnostic {
    return Diagnostic(
        range(err.location.toLocation()),
        err.message,
        DiagnosticSeverity.Warning,
        "jitsu"
    )
}