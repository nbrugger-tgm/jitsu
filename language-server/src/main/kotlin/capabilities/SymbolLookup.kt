package capabilities

import eu.nitok.jitsu.compiler.ast.StatementNode.Declaration.*
import eu.nitok.jitsu.compiler.ast.StatementNode.InstructionNode.*
import eu.nitok.jitsu.compiler.ast.StatementNode.NamedTypeDeclarationNode
import eu.nitok.jitsu.compiler.graph.*
import eu.nitok.jitsu.compiler.graph.Function
import eu.nitok.jitsu.compiler.model.mapTree
import eu.nitok.jitsu.compiler.model.sequence
import eu.nitok.jitsu.compiler.parser.Range
import getArtificalId
import org.eclipse.lsp4j.DocumentSymbol
import org.eclipse.lsp4j.SymbolKind
import range


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
                    "${node.enum.name.value}.${node}",
                    children.toList()
                )
            )
        }
    }.toList()
}

        is LineCommentNode -> listOf()
        is YieldStatement -> expression?.documentSymbols()?: listOf()
    }
}

private fun NamedTypeDeclarationNode.documentSymbols(): List<DocumentSymbol> {
    return when (this) {
        is NamedTypeDeclarationNode.EnumDeclarationNode -> {
            val constantSymbols = constants.map {
                DocumentSymbol(
                    node.name.value,
                    SymbolKind.Variable,
                    range(node.name.location),
                    range(node.name.location),
                    node.declaredType.toString(),
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

//fun Statement.documentSymbols(): List<DocumentSymbol> {
//    return when (this) {
//        is AssignmentNode -> value?.documentSymbols() ?: listOf()
//        is CodeBlockNode.SingleExpressionCodeBlock -> expression.documentSymbols()
//        is CodeBlockNode.StatementsCodeBlock -> statements.flatMap { it.documentSymbols() }
//        is FunctionCallNode -> listOf()
//        is FunctionDeclarationNode ->
//
//        is IfNode -> {
//            val elseNodes = elseStatement?.let {
//                when (it) {
//                    is IfNode.ElseNode.ElseBlockNode -> (it.codeBlock as StatementNode).documentSymbols()
//                    is IfNode.ElseNode.ElseIfNode -> (it.ifNode as StatementNode).documentSymbols()
//                }
//            } ?: emptyList();
//            (condition?.documentSymbols()
//                ?: listOf()) + (thenCodeBlockNode as StatementNode).documentSymbols() + elseNodes
//        }
//
//        is MethodInvocationNode -> method.documentSymbols() + parameters.flatMap { it.documentSymbols() }
//        is ReturnNode -> expression?.documentSymbols() ?: emptyList()
//        is SwitchNode -> (item?.documentSymbols() ?: listOf()) + cases.flatMap { it.documentSymbols() }
//        is NamedTypeDeclarationNode -> documentSymbols()
//        is VariableDeclarationNode -> listOf(
//            DocumentSymbol(
//                name?.value ?: "",
//                SymbolKind.Variable,
//                range(location),
//                range(name?.location ?: location),
//                type.toString(),
//                value?.documentSymbols() ?: emptyList()
//            )
//        )
//
//        is LineCommentNode -> listOf()
//        is YieldStatement -> expression?.documentSymbols() ?: listOf()
//    }
//}
//
//private fun NamedTypeDeclarationNode.documentSymbols(): List<DocumentSymbol> {
//    return when (this) {
//        is NamedTypeDeclarationNode.EnumDeclarationNode -> {
//            val constantSymbols = constants.map {
//                DocumentSymbol(
//                    it.value,
//                    SymbolKind.EnumMember,
//                    range(it.location),
//                    range(it.location),
//                    "${name}.${it}",
//                )
//            }
//            listOf(
//                DocumentSymbol(
//                    name.value,
//                    SymbolKind.Enum,
//                    range(location),
//                    range(name.location),
//                    "enum $name (${constants.size} options)",
//                    constantSymbols
//                )
//            )
//        }
//
//        is NamedTypeDeclarationNode.InterfaceTypeNode -> listOf(
//            DocumentSymbol(
//                name.value,
//                SymbolKind.Interface,
//                range(location),
//                range(name.location),
//                null,
//                this.functions.flatMap { it.documentSymbols() }
//            )
//        )
//
//        is NamedTypeDeclarationNode.TypeAliasNode -> this.type.documentSymbols(
//            this.location,
//            this.name.value,
//            this.name.location
//        )
//    }
//}
//
//private fun TypeNode.documentSymbols(location: Range, name: String, nameLocation: Range): List<DocumentSymbol> {
//    return when (this) {
//        is TypeNode.FloatTypeNode,
//        is TypeNode.IntTypeNode,
//        is TypeNode.NameTypeNode,
//        is TypeNode.ValueTypeNode,
//        is TypeNode.UIntTypeNode -> listOf(
//            DocumentSymbol(
//                name,
//                SymbolKind.Variable,
//                range(location),
//                range(nameLocation),
//                this::class.simpleName
//            )
//        )
//
//        is TypeNode.ArrayTypeNode -> listOf(
//            DocumentSymbol(
//                name,
//                SymbolKind.Array,
//                range(location),
//                range(nameLocation),
//                this::class.simpleName
//            )
//        )
//
//        is TypeNode.UnionTypeNode -> listOf(
//            DocumentSymbol(
//                name,
//                SymbolKind.Enum,
//                range(location),
//                range(nameLocation),
//                this::class.simpleName
//            )
//        )
//
//        is TypeNode.FunctionTypeSignatureNode -> listOf(
//            DocumentSymbol(
//                name,
//                SymbolKind.Function,
//                range(location),
//                range(nameLocation),
//                toString()
//            )
//        )
//
//        is TypeNode.VoidTypeNode -> listOf()
//        is TypeNode.StructuralInterfaceTypeNode -> this.fields.map {
//            DocumentSymbol(
//                it.name.value,
//                SymbolKind.Field,
//                range(it.location),
//                range(it.name.location),
//                it.toString(),
//                it.type?.let { documentSymbols(it.location, "anonymous\$si\$${getArtificalId()}", it.location) }
//                    ?: listOf()
//            )
//        }
//    }
//}
//
//private fun NamedTypeDeclarationNode.InterfaceTypeNode.documentSymbol(): List<DocumentSymbol> {
//    return listOf(DocumentSymbol(
//        name.value,
//        SymbolKind.Interface,
//        range(location),
//        range(name.location),
//        "interface",
//        functions.flatMap { it.documentSymbols() }
//    ))
//}
//
//private fun NamedTypeDeclarationNode.InterfaceTypeNode.FunctionSignatureNode.documentSymbols(): List<DocumentSymbol> {
//    return listOf(
//        DocumentSymbol(
//            name.value,
//            SymbolKind.Method,
//            range(location),
//            range(name.location),
//            typeSignature.toString(),
//            listOf()
//        )
//    )
//}
//
//private fun SwitchNode.CaseNode.documentSymbols(): List<DocumentSymbol> {
//    //https://discuss.kotlinlang.org/t/what-is-the-reason-behind-smart-cast-being-impossible-to-perform-when-referenced-class-is-in-another-module/2201/36
//    return when (val body = body) {
//        is CodeBlockNode -> (body as StatementNode).documentSymbols()
//        is ExpressionNode -> body.documentSymbols()
//        null -> listOf()
//    }
//}
//
//private fun FunctionDeclarationNode.ParameterNode.documentSymbols(): List<DocumentSymbol> {
//    return listOf(
//        DocumentSymbol(
//            name.value,
//            SymbolKind.Variable,
//            range(location),
//            range(name.location),
//            type.toString(),
//            emptyList()
//        )
//    )
//}
//
//private fun ExpressionNode.documentSymbols(): List<DocumentSymbol> {
//    return when (this) {
//        is ExpressionNode.BooleanLiteralNode -> emptyList()
//        is ExpressionNode.NumberLiteralNode.FloatLiteralNode -> listOf()
//        is ExpressionNode.NumberLiteralNode.IntegerLiteralNode -> listOf()
//        is ExpressionNode.OperationNode -> left.documentSymbols() + (right?.documentSymbols() ?: listOf())
//        is ExpressionNode.StringLiteralNode -> emptyList()
//        is ExpressionNode.VariableReferenceNode -> listOf()
//        is ExpressionNode.FieldAccessNode -> emptyList()
//        is ExpressionNode.IndexAccessNode -> emptyList()
//        is StatementNode -> (this as StatementNode).documentSymbols()
//    }
//}


