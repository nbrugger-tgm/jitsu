package capabilities

import eu.nitok.jitsu.compiler.graph.*
import eu.nitok.jitsu.compiler.graph.Function
import eu.nitok.jitsu.compiler.model.mapTree
import org.eclipse.lsp4j.DocumentSymbol
import org.eclipse.lsp4j.SymbolKind
import range


private fun TypeDefinition.documentSymbols(children: Iterable<DocumentSymbol>): List<DocumentSymbol> {
    return when (this) {
        is TypeDefinition.Enum -> {
            listOf(
                DocumentSymbol(
                    name.value,
                    SymbolKind.Enum,
                    range(name.location),
                    range(name.location),
                    "enum $name (${constants.size} options)",
                    children.toList()
                )
            )
        }

        is TypeDefinition.Interface -> listOf(
            DocumentSymbol(
                name.value,
                SymbolKind.Interface,
                range(name.location),
                range(name.location),
                null,
                children.toList()
            )
        )

        is TypeDefinition.Alias -> listOf(
            DocumentSymbol(
                name.value,
                this.type.value.resolveTypeKind(),
                range(name.location),
                range(name.location),
                null,
                children.toList()
            )
        )

        is TypeDefinition.Struct -> TODO()
    }
}
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
    }
}

fun JitsuFile.documentSymbols(): List<DocumentSymbol> {
    return this.mapTree { node, children ->
        if (node !is Accessible<*>) return@mapTree children;
        when (node) {
            is Function -> node.documentSymbols(children)?.let { listOf(it) } ?: children
            is TypeDefinition.Alias -> TODO()
            is TypeDefinition.Enum -> TODO()
            is TypeDefinition.Interface -> TODO()
            is TypeDefinition.Struct -> TODO()
            is Variable -> listOf(
                DocumentSymbol(
                    node.name.value,
                    SymbolKind.Variable,
                    range(node.name.location),
                    range(node.name.location),
                    node.declaredType.toString(),
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


