package capabilities

import eu.nitok.jitsu.compiler.ast.*
import eu.nitok.jitsu.compiler.ast.StatementNode.SwitchNode.CaseNode.CaseBodyNode
import eu.nitok.jitsu.compiler.ast.StatementNode.SwitchNode.CaseNode.CaseMatchNode
import eu.nitok.jitsu.compiler.ast.StatementNode.SwitchNode.CaseNode.CaseMatchNode.ConditionCaseNode.CaseMatchingNode
import getArtificalId
import org.eclipse.lsp4j.DocumentSymbol
import org.eclipse.lsp4j.SymbolKind
import range

//
fun StatementNode.documentSymbols(): List<DocumentSymbol> {
    return when (this) {
        is StatementNode.AssignmentNode -> value.symbols { it.documentSymbols() }
        is StatementNode.CodeBlockNode.SingleExpressionCodeBlock -> expression.symbols { it.documentSymbols() }
        is StatementNode.CodeBlockNode.StatementsCodeBlock -> statements.flatMap { it.symbols { it.documentSymbols() } }
        is StatementNode.FunctionCallNode -> listOf()
        is StatementNode.FunctionDeclarationNode -> listOf(
            DocumentSymbol(
                orElse(name?.map { it.first }, "anonymous\$${getArtificalId()}"),
                SymbolKind.Function,
                range(location),
                range(location),
                "(${
                    parameters.joinToString(", ") { it.toString() }
                }) ${if (returnType != null) ": $returnType" else ""}",
                parameters.flatMap { it.symbols { it.documentSymbols() } } +
                        body.symbols { (it as StatementNode).documentSymbols() }
            )
        )

        is StatementNode.IfNode -> {
            val elseNodes = elseStatement?.symbols {
                it.let {
                    when (it) {
                        is StatementNode.IfNode.ElseNode.ElseBlockNode -> it.codeBlock.symbols { (it as StatementNode).documentSymbols() }
                        is StatementNode.IfNode.ElseNode.ElseIfNode -> it.ifNode.symbols { (it as StatementNode).documentSymbols() }
                    }
                }
            } ?: emptyList();
            return (condition.symbols { it.documentSymbols() } + thenCodeBlockNode.symbols { (it as StatementNode).documentSymbols() } + elseNodes)
        }

        is StatementNode.MethodInvocationNode -> method.symbols { it.documentSymbols() } + parameters.flatMap { it.symbols { it.documentSymbols() } }
        is StatementNode.ReturnNode -> expression?.symbols { it.documentSymbols() } ?: emptyList()
        is StatementNode.SwitchNode -> item.symbols { it.documentSymbols() } + cases.flatMap { it.symbols { it.documentSymbols() } }
        is StatementNode.TypeDefinitionNode -> type.symbols {
            it.documentSymbols(
                location,
                name.map { it.first }.toString(),
                name.location
            )
        }

        is StatementNode.VariableDeclarationNode -> listOf(
            DocumentSymbol(
                name.map { it.first }.toString(),
                SymbolKind.Variable,
                range(location),
                range(name.location),
                type.toString(),
                (value?.symbols { it.documentSymbols() } ?: emptyList()) + (type?.symbols {
                    it.documentSymbols(
                        it.location,
                        "anonymous\$${getArtificalId()}",
                        it.location
                    )
                } ?: emptyList())
            )
        )

        is StatementNode.LineCommentNode -> listOf()
        is StatementNode.YieldStatement -> expression.symbols { it.documentSymbols() }
        is TypeNode.InterfaceTypeNode -> documentSymbol()
    }
}

private fun <T> N<T>.symbols(mapper: (T) -> List<DocumentSymbol>): List<DocumentSymbol> {
    return when (this) {
        is N.Node -> mapper(this.value)
        is N.Error -> listOf()
    }
}

private fun TypeNode.documentSymbols(location: Location, name: String, nameLocation: Location): List<DocumentSymbol> {
    return when (this) {
        is TypeNode.FloatTypeNode,
        is TypeNode.IntTypeNode,
        is TypeNode.NamedTypeNode,
        is TypeNode.StringTypeNode,
        is TypeNode.ValueTypeNode -> listOf(
            DocumentSymbol(
                name,
                SymbolKind.Variable,
                range(location),
                range(nameLocation),
                this::class.simpleName
            )
        )
        is TypeNode.ArrayTypeNode -> listOf(
            DocumentSymbol(
                name,
                SymbolKind.Array,
                range(location),
                range(nameLocation),
                this::class.simpleName
            )
        )
        is TypeNode.UnionTypeNode -> listOf(
            DocumentSymbol(
                name,
                SymbolKind.Enum,
                range(location),
                range(nameLocation),
                this::class.simpleName
            )
        )
        is TypeNode.EnumDeclarationNode -> {
            val constantSymbols = constants.map {
                DocumentSymbol(
                    it.map { it.name }.toString(),
                    SymbolKind.EnumMember,
                    range(it.location { it.location }),
                    range(it.location { it.location }),
                    "${name}.${it}",
                    mutableListOf()
                )
            }
            listOf(
                DocumentSymbol(
                    name,
                    SymbolKind.Enum,
                    range(location),
                    range(nameLocation),
                    "enum $name (${constants.size} options)",
                    constantSymbols
                )
            )
        }

        is TypeNode.FunctionTypeSignatureNode -> listOf(DocumentSymbol(
                name,
                SymbolKind.Function,
                range(location),
                range(nameLocation),
                toString(),
                listOf()
        ))
        is TypeNode.InterfaceTypeNode -> listOf(
            DocumentSymbol(
                name,
                SymbolKind.Interface,
                range(location),
                range(nameLocation),
                null,
                documentSymbol()
            )
        )
        is TypeNode.VoidTypeNode -> listOf()
    }
}

private fun TypeNode.InterfaceTypeNode.documentSymbol(): List<DocumentSymbol> {
    return this.name?.let {
        listOf(DocumentSymbol(
            it.map { it.first }.toString(),
            SymbolKind.Interface,
            range(location),
            range(it.location),
            "interface",
            functions.flatMap { it.symbols { it.documentSymbols() } }
        ))
    } ?: functions.flatMap { it.symbols { it.documentSymbols() } }
}

private fun TypeNode.InterfaceTypeNode.FunctionSignatureNode.documentSymbols(): List<DocumentSymbol> {
    return listOf(
        DocumentSymbol(
            name.map { it.first }.toString(),
            SymbolKind.Method,
            range(location),
            range(name.location),
            typeSignature.toString(),
            listOf()
        )
    )
}

private fun StatementNode.SwitchNode.CaseNode.documentSymbols(): List<DocumentSymbol> {
    //https://discuss.kotlinlang.org/t/what-is-the-reason-behind-smart-cast-being-impossible-to-perform-when-referenced-class-is-in-another-module/2201/36
    return when (val matcher = matcher) {
        is CaseMatchNode.ConditionCaseNode -> {
            matcher.matching.symbols {
                when (val matching = it) {
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
        }

        is CaseMatchNode.ConstantCaseNode -> listOf()
        is CaseMatchNode.DefaultCaseNode -> listOf()
    } + when (val body = body) {
        is CaseBodyNode.CodeBlockCaseBodyNode -> body.codeBlock.symbols { (it as StatementNode).documentSymbols() }
        is CaseBodyNode.ExpressionCaseBodyNode -> body.expression.symbols { it.documentSymbols() }
    }
}

private fun StatementNode.FunctionDeclarationNode.ParameterNode.documentSymbols(): List<DocumentSymbol> {
    return listOf(
        DocumentSymbol(
            name.map { it.first }.toString(),
            SymbolKind.Variable,
            range(location),
            range(name.location),
            type.toString(),
            emptyList()
        )
    )
}

private fun ExpressionNode.documentSymbols(): List<DocumentSymbol> {
    return when (this) {
        is ExpressionNode.BooleanLiteralNode -> emptyList()
        is ExpressionNode.NumberLiteralNode.FloatLiteralNode -> listOf()
        is ExpressionNode.NumberLiteralNode.IntegerLiteralNode -> listOf()
        is ExpressionNode.OperationNode -> left.symbols { it.documentSymbols() } + right.symbols { it.documentSymbols() }
        is ExpressionNode.StringLiteralNode -> emptyList()
        is ExpressionNode.VariableLiteralNode -> listOf()
        is ExpressionNode.FieldAccessNode -> emptyList()
        is ExpressionNode.IndexAccessNode -> emptyList()
        is StatementNode -> (this as StatementNode).documentSymbols()
    }
}


