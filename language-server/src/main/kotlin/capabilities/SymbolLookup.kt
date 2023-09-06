package capabilities

import eu.nitok.jitsu.compiler.ast.ExpressionNode
import eu.nitok.jitsu.compiler.ast.Location
import eu.nitok.jitsu.compiler.ast.StatementNode
import eu.nitok.jitsu.compiler.ast.StatementNode.SwitchNode.CaseNode.CaseBodyNode
import eu.nitok.jitsu.compiler.ast.StatementNode.SwitchNode.CaseNode.CaseMatchNode
import eu.nitok.jitsu.compiler.ast.StatementNode.SwitchNode.CaseNode.CaseMatchNode.ConditionCaseNode.CaseMatchingNode
import eu.nitok.jitsu.compiler.ast.TypeNode
import getArtificalId
import org.eclipse.lsp4j.DocumentSymbol
import org.eclipse.lsp4j.SymbolKind
import range

/**
 *
 * TODO: DO NOT! consider references but only declarations!
 *
 */


fun StatementNode.documentSymbols(): List<DocumentSymbol> {
    return when (this) {
        is StatementNode.AssignmentNode -> value.documentSymbols()
        is StatementNode.CodeBlockNode.SingleExpressionCodeBlock -> expression.documentSymbols()
        is StatementNode.CodeBlockNode.StatementsCodeBlock -> statements.flatMap { it.documentSymbols() }
        is StatementNode.FunctionCallNode -> listOf()
        is StatementNode.FunctionDeclarationNode -> listOf(
            DocumentSymbol(
                name ?: "anonymous\$${getArtificalId()}",
                SymbolKind.Function,
                range(location),
                range(location),
                "fn ${name} (${
                    parameters.map { "${it.name}: ${it.type}" }.joinToString(", ")
                }) ${if (returnType != null) ":" + returnType else ""}",
                parameters.flatMap { it.documentSymbols() }  + body.documentSymbols()
            )
        )

        is StatementNode.IfNode -> {
            val elseNodes = elseStatement?.let {
                when (it) {
                    is StatementNode.IfNode.ElseNode.ElseBlockNode -> it.codeBlock.documentSymbols()
                    is StatementNode.IfNode.ElseNode.ElseIfNode -> it.ifNode.documentSymbols()
                }
            } ?: emptyList();
            return (condition.documentSymbols() + thenCodeBlockNode.documentSymbols() + elseNodes)
        }

        is StatementNode.MethodInvocationNode -> target.documentSymbols() + parameters.flatMap { it.documentSymbols() }
        is StatementNode.ReturnNode -> expression?.documentSymbols() ?: emptyList()
        is StatementNode.SwitchNode -> item.documentSymbols() + cases.flatMap { it.documentSymbols() }
        is StatementNode.TypeDefinitionNode -> type.documentSymbols(location, name, nameLocation)

        is StatementNode.VariableDeclarationNode -> listOf(
            DocumentSymbol(
                name,
                SymbolKind.Variable,
                range(location),
                range(nameLocation),
                "${name} : ${type}",
                emptyList()
            ),
            *value?.documentSymbols()?.toTypedArray() ?: emptyArray()
        )
    }
}

private fun TypeNode.documentSymbols(location: Location, name: String, nameLocation: Location): List<DocumentSymbol> {
    return when (this) {
        is TypeNode.ArrayTypeNode,
        is TypeNode.FloatTypeNode,
        is TypeNode.IntTypeNode,
        is TypeNode.NamedTypeNode,
        is TypeNode.StringTypeNode,
        is TypeNode.UnionTypeNode,
        is TypeNode.ValueTypeNode -> listOf(DocumentSymbol(name, SymbolKind.Class, range(location), range(nameLocation), this::class.simpleName))
        is TypeNode.EnumDeclarationNode -> {
            val constantSymbols = constants.map {
                DocumentSymbol(
                    it.name,
                    SymbolKind.EnumMember,
                    range(it.location),
                    range(it.location),
                    "${name}.${it}",
                    mutableListOf()
                )
            }
            listOf(DocumentSymbol(
                name,
                SymbolKind.Enum,
                range(location),
                range(nameLocation),
                "enum $name (${constants.size} options)",
                constantSymbols
            ))
        }
    }
}

private fun StatementNode.SwitchNode.CaseNode.documentSymbols(): List<DocumentSymbol> {
    //https://discuss.kotlinlang.org/t/what-is-the-reason-behind-smart-cast-being-impossible-to-perform-when-referenced-class-is-in-another-module/2201/36
    return when (val matcher = matcher) {
        is CaseMatchNode.ConditionCaseNode -> {
            when (val matching = matcher.matching) {
                is CaseMatchingNode.CastingPatternMatch -> listOf(
                    DocumentSymbol(
                        matching.captureName,
                        SymbolKind.Variable,
                        range(matcher.location),
                        range(matching.location),
                        "${matching.captureName} : ${matcher.type}",
                        listOf()
                    )
                )

                is CaseMatchingNode.DeconstructPatternMatch -> matching.variables.map {
                    DocumentSymbol(
                        it.name,
                        SymbolKind.Variable,
                        range(matcher.location),
                        range(it.location),
                        "$it : ${matcher.type}",
                        listOf()
                    )
                }
            }
        }

        is CaseMatchNode.ConstantCaseNode -> listOf()
        is CaseMatchNode.DefaultCaseNode -> listOf()
    } + when (val body = body) {
        is CaseBodyNode.CodeBlockCaseBodyNode -> body.codeBlock.documentSymbols()
        is CaseBodyNode.ExpressionCaseBodyNode -> body.expression.documentSymbols()
    }
}

private fun StatementNode.FunctionDeclarationNode.ParameterNode.documentSymbols(): List<DocumentSymbol> {
    return listOf(
        DocumentSymbol(
            name,
            SymbolKind.Variable,
            range(location),
            range(nameLocation),
            "${name} : ${type}",
            emptyList()
        )
    )
}

private fun ExpressionNode.documentSymbols(): List<DocumentSymbol> {
    return when (this) {
        is ExpressionNode.BooleanLiteralNode -> emptyList()
        is ExpressionNode.NumberLiteralNode.FloatLiteralNode -> listOf()
        is ExpressionNode.NumberLiteralNode.IntegerLiteralNode -> listOf()
        is ExpressionNode.OperationNode -> left.documentSymbols() + right.documentSymbols()
        is ExpressionNode.StatementExpressionNode -> statement.documentSymbols()
        is ExpressionNode.StringLiteralNode -> emptyList()
        is ExpressionNode.VariableLiteralNode -> listOf()
        is ExpressionNode.FieldAccessNode -> emptyList()
    }
}


