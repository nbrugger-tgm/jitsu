package capabilities

import eu.nitok.jitsu.compiler.graph.*
import eu.nitok.jitsu.compiler.graph.Function
import eu.nitok.jitsu.compiler.model.mapTree
import org.eclipse.lsp4j.DocumentSymbol
import org.eclipse.lsp4j.SymbolKind
import range


private fun TypeDefinition.documentSymbols(children: Iterable<DocumentSymbol>): List<DocumentSymbol> = listOf(
    DocumentSymbol(
        name.value,
        symbolKind(),
        range(name.location),
        range(name.location),
        null,
        children.toList()
    )
)


private fun TypeDefinition.symbolKind() = when (this) {
    is TypeDefinition.Enum -> SymbolKind.Enum
    is TypeDefinition.Interface -> SymbolKind.Interface
    is TypeDefinition.Alias -> type.value.resolveTypeKind()
    is TypeDefinition.Struct -> SymbolKind.Struct
}

private fun Type.resolveTypeKind(): SymbolKind? {
    return when (this) {
        is Type.Float,
        is Type.Int,
        is Type.UInt -> SymbolKind.Number

        is Type.Value -> SymbolKind.Constant
        is Type.Array -> SymbolKind.Array
        Type.Boolean -> SymbolKind.Boolean
        is Type.FunctionTypeSignature -> SymbolKind.Function
        Type.Null -> SymbolKind.Null
        is Type.TypeReference -> target?.symbolKind()
        Type.Undefined -> SymbolKind.Null
        is Type.Union -> SymbolKind.Enum
    }
}

fun JitsuFile.documentSymbols(): List<DocumentSymbol> {
    return this.mapTree { node, children ->
        if (node !is Accessible<*>) return@mapTree children;
        when (node) {
            is Function -> node.documentSymbols(children)?.let { listOf(it) } ?: children
            is TypeDefinition -> node.documentSymbols(children)
            is Variable -> listOf(
                DocumentSymbol(
                    node.name.value,
                    SymbolKind.Variable,
                    range(node.name.location),
                    range(node.name.location),
                    node.type.toString(),
                    children.toList()
                )
            )

            is TypeDefinition.Enum.Constant -> listOf(
                DocumentSymbol(
                    node.name.value,
                    SymbolKind.EnumMember,
                    range(node.name.location),
                    range(node.name.location),
                    null,
                    children.toList()
                )
            )
        }
    }.toList()
}

fun Function.documentSymbols(children: Iterable<DocumentSymbol>): DocumentSymbol? {
    return if (name != null) DocumentSymbol(
        name!!.value,
        SymbolKind.Function,
        range(name!!.location),
        range(name!!.location),
        "(${
            parameters.joinToString(", ") { it.toString() }
        }) ${if (returnType != null) " -> $returnType" else ""}",
        children.toList()
    ) else null
}


