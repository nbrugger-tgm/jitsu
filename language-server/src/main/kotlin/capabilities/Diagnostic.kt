package capabilities

import eu.nitok.jitsu.compiler.ast.*
import eu.nitok.jitsu.compiler.ast.StatementNode.AssignmentNode.AssignmentTarget.FieldTarget
import eu.nitok.jitsu.compiler.ast.StatementNode.AssignmentNode.AssignmentTarget.VariableTarget
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import range

private fun <T> N<T>.unwarp(function: (T) -> List<Diagnostic>): List<Diagnostic> {//rewrite to this form
    return when (this) {
        is N.Node -> function(this.value) + when (val withAttrs = this.value) {
            is CanHaveAttributes -> withAttrs.attributes.flatMap { it.unwarp { syntaxDiagnostic(it) } }
            else -> listOf()
        }

        is N.Error -> listOf(error(this))
    } + this.warnings.map { error(it) }
}

fun syntaxDiagnostic(it: AttributeNode): List<Diagnostic> {
    return it.name.unwarp { listOf() } + it.values.flatMap { it.unwarp { syntaxDiagnostic(it) } }
}

fun syntaxDiagnostic(it: AttributeNode.AttributeValueNode): List<Diagnostic> {
    return it.name.unwarp { listOf() } + it.value.unwarp { syntaxDiagnostic(it) }
}

public fun syntaxDiagnostic(rawAst: N<StatementNode>): List<Diagnostic> {
    return rawAst.unwarp { syntaxDiagnostic(it) }
}

internal fun syntaxDiagnostic(rawAst: StatementNode): List<Diagnostic> {
    return when (rawAst) {
        is StatementNode.AssignmentNode -> rawAst.value.unwarp { syntaxDiagnostic(it) } + rawAst.target.unwarp {
            syntaxDiagnostic(
                it
            )
        }//this is the needed form
        is StatementNode.CodeBlockNode.SingleExpressionCodeBlock -> rawAst.expression.unwarp { syntaxDiagnostic(it) }
        is StatementNode.CodeBlockNode.StatementsCodeBlock -> rawAst.statements.flatMap {
            it.unwarp { innerIt ->
                syntaxDiagnostic(
                    innerIt
                )
            }
        }

        is StatementNode.FunctionCallNode -> rawAst.function.unwarp { syntaxDiagnostic(it) } +
                rawAst.parameters.flatMap {
                    it.unwarp { innerIt -> syntaxDiagnostic(innerIt) }
                }

        is StatementNode.FunctionDeclarationNode -> {
            rawAst.body.unwarp { syntaxDiagnostic(it as StatementNode) } +
                    rawAst.parameters.flatMap { it.unwarp { innerIt -> syntaxDiagnostic(innerIt) } } +
                    (rawAst.returnType?.unwarp { syntaxDiagnostic(it) } ?: listOf()) +
                    (rawAst.name?.unwarp { listOf() } ?: listOf())
        }

        is StatementNode.IfNode -> rawAst.condition.unwarp { syntaxDiagnostic(it) } +
                rawAst.thenCodeBlockNode.unwarp { syntaxDiagnostic(it as StatementNode) } +
                (rawAst.elseStatement?.unwarp { syntaxDiagnostic(it) } ?: listOf())

        is StatementNode.LineCommentNode -> listOf()
        is StatementNode.MethodInvocationNode -> rawAst.method.unwarp { syntaxDiagnostic(it) } +
                rawAst.method.unwarp { listOf() } +
                rawAst.parameters.flatMap { param -> param.unwarp { syntaxDiagnostic(it) } }

        is StatementNode.ReturnNode -> rawAst.expression?.unwarp { syntaxDiagnostic(it) } ?: listOf()
        is StatementNode.SwitchNode -> rawAst.item.unwarp { syntaxDiagnostic(it) } +
                rawAst.cases.flatMap { it.unwarp { innerIt -> syntaxDiagnostic(innerIt) } }

        is StatementNode.TypeDefinitionNode -> rawAst.type.unwarp { syntaxDiagnostic(it) } + rawAst.name.unwarp { listOf() }
        is StatementNode.VariableDeclarationNode -> rawAst.name.unwarp { listOf() } +
                (rawAst.type?.unwarp { syntaxDiagnostic(it) } ?: listOf()) +
                (rawAst.value?.unwarp { syntaxDiagnostic(it) } ?: listOf())

        is StatementNode.YieldStatement -> rawAst.expression.unwarp { syntaxDiagnostic(it) }
        is TypeNode.InterfaceTypeNode -> syntaxDiagnostic(rawAst as TypeNode)
    }
}

fun syntaxDiagnostic(it: StatementNode.SwitchNode.CaseNode): List<Diagnostic> {
    TODO("Not yet implemented")
}

fun syntaxDiagnostic(node: StatementNode.IfNode.ElseNode): List<Diagnostic> {
    return when (node) {
        is StatementNode.IfNode.ElseNode.ElseBlockNode -> node.codeBlock.unwarp { syntaxDiagnostic(it as StatementNode) }
        is StatementNode.IfNode.ElseNode.ElseIfNode -> node.ifNode.unwarp { syntaxDiagnostic(it as StatementNode) }
    }
}

fun syntaxDiagnostic(it: TypeNode.InterfaceTypeNode.FunctionSignatureNode): List<Diagnostic> {
    return it.name.unwarp { listOf() } + syntaxDiagnostic(it.typeSignature)
}

fun syntaxDiagnostic(it: TypeNode.FunctionTypeSignatureNode): List<Diagnostic> {
    return (it.returnType?.unwarp { syntaxDiagnostic(it) } ?: listOf()) +
            it.parameters.flatMap { it.unwarp { syntaxDiagnostic(it) } }
}


fun syntaxDiagnostic(node: TypeNode): List<Diagnostic> {
    return when (node) {
        is TypeNode.ArrayTypeNode -> node.type.unwarp { syntaxDiagnostic(it) } +
                (node.fixedSize?.unwarp { listOf() } ?: listOf())

        is TypeNode.EnumDeclarationNode -> node.constants.flatMap {
            it.unwarp { listOf() }
        }

        is TypeNode.FloatTypeNode -> listOfNotNull(error(node.bitSize))
        is TypeNode.IntTypeNode -> listOfNotNull(error(node.bitSize))
        is TypeNode.NamedTypeNode -> node.name.unwarp { listOf() } +
                node.genericTypes.flatMap { it.unwarp { inner -> syntaxDiagnostic(inner) } }

        is TypeNode.StringTypeNode -> listOf()
        is TypeNode.UnionTypeNode -> node.types.flatMap { type -> type.unwarp { syntaxDiagnostic(it) } }
        is TypeNode.ValueTypeNode -> node.value.unwarp { syntaxDiagnostic(it) }
        is TypeNode.FunctionTypeSignatureNode -> syntaxDiagnostic(node)
        is TypeNode.InterfaceTypeNode -> node.functions.flatMap { it.unwarp { syntaxDiagnostic(it) } } +
                (node.name?.unwarp { listOf() } ?: emptyList())

        is TypeNode.VoidTypeNode -> listOf()
    }
}

fun syntaxDiagnostic(param: StatementNode.FunctionDeclarationNode.ParameterNode): List<Diagnostic> {
    return (param.defaultValue?.unwarp { syntaxDiagnostic(it) } ?: listOf()) +
            param.name.unwarp { listOf() } +
            param.type.unwarp { syntaxDiagnostic(it) }
}

fun syntaxDiagnostic(target: StatementNode.AssignmentNode.AssignmentTarget): List<Diagnostic> {
    return when (target) {
        is FieldTarget -> target.field.unwarp { syntaxDiagnostic(it) }
        is VariableTarget -> listOf()
        is StatementNode.AssignmentNode.AssignmentTarget.IndexAccessTarget -> target.target.unwarp { syntaxDiagnostic(it) }
    }
}

fun syntaxDiagnostic(rawAst: ExpressionNode): List<Diagnostic> {
    return when (rawAst) {
        is ExpressionNode.OperationNode -> rawAst.left.unwarp { syntaxDiagnostic(it) } + rawAst.right.unwarp {
            syntaxDiagnostic(
                it
            )
        }

        is ExpressionNode.StringLiteralNode -> rawAst.content.flatMap {
            it.unwarp {
                when (it) {
                    is ExpressionNode.StringLiteralNode.StringPart.Literal -> listOf()
                    is ExpressionNode.StringLiteralNode.StringPart.Expression -> it.expression.unwarp {
                        syntaxDiagnostic(
                            it
                        )
                    }

                    is ExpressionNode.StringLiteralNode.StringPart.Charsequence -> listOf()
                    is ExpressionNode.StringLiteralNode.StringPart.EscapeSequence -> listOf()
                }
            }
        }

        is ExpressionNode.BooleanLiteralNode,
        is ExpressionNode.NumberLiteralNode.FloatLiteralNode,
        is ExpressionNode.NumberLiteralNode.IntegerLiteralNode,
        is ExpressionNode.VariableLiteralNode -> listOf()

        is ExpressionNode.FieldAccessNode -> rawAst.target.unwarp { syntaxDiagnostic(it) } + rawAst.field.unwarp { listOf() }
        is ExpressionNode.IndexAccessNode -> rawAst.target.unwarp { syntaxDiagnostic(it) } + rawAst.index.unwarp {
            syntaxDiagnostic(
                it
            )
        }

        is StatementNode -> syntaxDiagnostic(rawAst as StatementNode)
    }
}

fun error(err: N.Error<*>): Diagnostic {
    return Diagnostic(
        range(err.explicitErrorLocation ?: err.node),
        err.message,
        DiagnosticSeverity.Error,
        "jainparse"
    )
}

fun error(err: N<*>): Diagnostic? {
    return when (err) {
        is N.Node -> null
        is N.Error -> error(err)
    }
}