package capabilities

import eu.nitok.jitsu.compiler.ast.AstNode
import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage
import flatMap
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import range

fun syntaxDiagnostic(node: AstNode): List<Diagnostic> {
    return node.flatMap {
        val warningDiagnostics = it.warnings.map { errorDiagnostic(it) }
        val errorDiagnostics = it.errors.map { errorDiagnostic(it) }
        return@flatMap warningDiagnostics + errorDiagnostics;
    }
}

private fun errorDiagnostic(err: CompilerMessage): Diagnostic {
    return Diagnostic(
        range(err.location.toRange()),
        err.message,
        DiagnosticSeverity.Error,
        "SRC ?? jainparse"
    )
}