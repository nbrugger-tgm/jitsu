package capabilities

import eu.nitok.jitsu.common.CompilerMessage
import eu.nitok.jitsu.common.flatMap
import eu.nitok.jitsu.compiler.graph.api.JitsuFile
import eu.nitok.jitsu.compiler.graph.api.JitsuModule
import eu.nitok.jitsu.parser.ast.AstNode
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

fun graphDiagnostic(scope: JitsuModule): Map<String, List<Diagnostic>> {
    return scope.messages.errors.groupBy({ it.location.file.toString() }, { errorDiagnostic(it) }) +
            scope.messages.warnings.groupBy({ it.location.file.toString() }, { warnDiagnostic(it) })
}

fun graphDiagnostic(scope: JitsuFile): List<Diagnostic> {
    return scope.module.messages.errors.filter {it.location.file == scope.uri }.map { errorDiagnostic(it) } +
            scope.module.messages.warnings.filter {it.location.file == scope.uri }.map { warnDiagnostic(it) }
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