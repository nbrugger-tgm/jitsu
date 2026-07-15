package capabilities

import eu.nitok.jitsu.common.CompilerMessage
import eu.nitok.jitsu.common.flatMap
import eu.nitok.jitsu.compiler.graph.api.JitsuModuleResult
import eu.nitok.jitsu.parser.ast.AstNode
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import range
import java.net.URI

fun syntaxDiagnostic(node: AstNode): List<Diagnostic> {
    return node.flatMap {
        val warningDiagnostics = it.warnings.map { warnDiagnostic(it) }
        val errorDiagnostics = it.errors.map { errorDiagnostic(it) }
        return@flatMap warningDiagnostics + errorDiagnostics;
    }
}

fun graphDiagnostic(result: JitsuModuleResult): Map<String, List<Diagnostic>> {
    return result.messages.errors.groupBy({ it.location.file.toString() }, { errorDiagnostic(it) }) +
            result.messages.warnings.groupBy({ it.location.file.toString() }, { warnDiagnostic(it) })
}

fun graphDiagnostic(result: JitsuModuleResult, uri: URI): List<Diagnostic> {
    return result.messages.errors.filter { it.location.file == uri }.map { errorDiagnostic(it) } +
            result.messages.warnings.filter { it.location.file == uri }.map { warnDiagnostic(it) }
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