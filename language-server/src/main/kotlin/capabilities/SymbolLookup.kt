package capabilities


import eu.nitok.jitsu.compiler.graph.*
import eu.nitok.jitsu.compiler.graph.Function
import eu.nitok.jitsu.compiler.graph.TypeDefinition.DirectTypeDefinition.Enum
import eu.nitok.jitsu.compiler.graph.TypeDefinition.ParameterizedType.Alias
import eu.nitok.jitsu.compiler.graph.TypeDefinition.ParameterizedType.Class
import eu.nitok.jitsu.compiler.graph.TypeDefinition.ParameterizedType.Interface
import eu.nitok.jitsu.compiler.graph.TypeDefinition.ParameterizedType.Struct
import eu.nitok.jitsu.compiler.model.mapTree
import org.eclipse.lsp4j.DocumentSymbol
import org.eclipse.lsp4j.SymbolKind
import range



private fun TypeDefinition.documentSymbols(children: Iterable<DocumentSymbol>): List<DocumentSymbol> = listOf(
    DocumentSymbol(
        this.toString(),
        symbolKind(),
        range(name.location),
        range(name.location),
        additionalInfo(),
        children.toList()
    )
)

private fun TypeDefinition.additionalInfo(): String?{
    if(this is Alias){
        return this.type.additionalInfo()
    }
    return null;
}
private fun Type.additionalInfo(): String?{
    return when(this) {
        is Type.TypeReference -> if(resolvedCache != this) resolvedCache.additionalInfo() else target.toString()
        else -> toString()
    }
}
private fun TypeDefinition.symbolKind() = when (this) {
    is Enum -> SymbolKind.Enum
    is Interface -> SymbolKind.Interface
    is Alias -> type.resolveTypeKind()
    is Struct -> SymbolKind.Struct
    is Class -> SymbolKind.Class
    is TypeDefinition.TypeParameter -> SymbolKind.TypeParameter
}

private fun Type.resolveTypeKind(): SymbolKind {
    return when (this) {
        is Type.Float,
        is Type.Int,
        is Type.UInt -> SymbolKind.Number

        is Type.Value -> SymbolKind.Constant
        is Type.Array -> SymbolKind.Array
        Type.Boolean -> SymbolKind.Boolean
        is Type.FunctionTypeSignature -> SymbolKind.Function
        Type.Null -> SymbolKind.Null
        is Type.TypeReference -> target?.symbolKind()?: SymbolKind.Null
        Type.Undefined -> SymbolKind.Null
        is Type.Union -> SymbolKind.Enum
        is Type.StructuralInterface -> SymbolKind.Interface
        is Enum ->  SymbolKind.Enum
    }
}

fun JitsuFile.documentSymbols(): List<DocumentSymbol> {
    return this.mapTree { node, children ->
        if (node !is Accessible<*>) return@mapTree children;
        node.documentSymbols(children)
    }.toList()
}

private fun <T: Accessible<T>> Accessible<T>.documentSymbols(children: Iterable<DocumentSymbol>): Iterable<DocumentSymbol> {
    return when (this) {
        is Function -> documentSymbols(children)?.let { listOf(it) } ?: children
        is TypeDefinition -> documentSymbols(children)
        is Variable -> when(this) {
            is VariableDeclaration -> listOf(
                DocumentSymbol(
                    name.value,
                    SymbolKind.Variable,
                    range(name.location),
                    range(name.location),
                    declaredType.toString(),
                    children.toList()
                )
            )
            else -> listOf()
        }
        is Enum.Constant -> listOf(
            DocumentSymbol(
                name.value,
                SymbolKind.EnumMember,
                range(name.location),
                range(name.location),
                null,
                children.toList()
            )
        )

        is Struct.Field -> listOf(DocumentSymbol(
            name.value,
            SymbolKind.Field,
            range(name.location),
            range(name.location),
            type.toString(),
            children.toList()
        ))
    }
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



