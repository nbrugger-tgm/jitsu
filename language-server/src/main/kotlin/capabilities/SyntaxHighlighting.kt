package capabilities

import capabilities.SemanticTokenTypes.*
import eu.nitok.jitsu.compiler.ast.*
import eu.nitok.jitsu.compiler.ast.StatementNode.FunctionDeclarationNode.ParameterNode
import eu.nitok.jitsu.compiler.ast.StatementNode.SwitchNode.CaseNode.CaseBodyNode
import eu.nitok.jitsu.compiler.ast.StatementNode.SwitchNode.CaseNode.CaseMatchNode
import eu.nitok.jitsu.compiler.ast.StatementNode.SwitchNode.CaseNode.CaseMatchNode.ConditionCaseNode.CaseMatchingNode
import java.util.concurrent.atomic.AtomicInteger


internal enum class SemanticTokenTypes(val id: String) {
    NAMESPACE("namespace"),
    TYPE("type"),
    CLASS("class"),
    ENUM("enum"),
    INTERFACE("interface"),
    STRUCT("struct"),
    TYPEPARAMETER("typeParameter"),
    PARAMETER("parameter"),
    VARIABLE("variable"),
    PROPERTY("property"),
    ENUMMEMBER("enumMember"),
    EVENT("event"),
    FUNCTION("function"),
    METHOD("method"),
    MACRO("macro"),
    KEYWORD("keyword"),
    MODIFIER("modifier"),
    COMMENT("comment"),
    STRING("string"),
    NUMBER("number"),
    REGEXP("regexp"),
    OPERATOR("operator");

    fun bitflag(): Int {
        return 1 shl ordinal
    }
}

internal enum class SemanticTokenModifiers(val id: String) {
    DECLARATION("declaration"),
    DEFINITION("definition"),
    READONLY("readonly"),
    STATIC("static"),
    DEPRECATED("deprecated"),
    ABSTRACT("abstract"),
    ASYNC("async"),
    MODIFICATION("modification"),
    DOCUMENTATION("documentation"),
    DEFAULTLIBRARY("defaultLibrary");

    fun bitflag(): Int {
        return 1 shl ordinal
    }
}

private data class SemanticToken(
    val type: SemanticTokenTypes,
    val modifiers: Array<out SemanticTokenModifiers>,
    val pos: Location
) {
    fun decode(previousLine: Int, previousChar: Int): List<Int> {
        val lineDiff = pos.fromLine - previousLine
        return listOf(
            lineDiff,
            if (lineDiff > 0) pos.fromColumn - 1 else pos.fromColumn - previousChar,
            pos.toColumn - pos.fromColumn + 1,
            type.ordinal,
            modifyerBitflag(*modifiers)
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SemanticToken) return false

        if (type != other.type) return false
        if (!modifiers.contentEquals(other.modifiers)) return false
        if (pos != other.pos) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + modifiers.contentHashCode()
        result = 31 * result + pos.hashCode()
        return result
    }
}

private fun decode(tokens: List<SemanticToken>): List<Int> {
    val previousLine = AtomicInteger(1)
    val previousChar = AtomicInteger(1)
    return tokens.sortedWith { a, b -> Location.byStart.compare(a.pos, b.pos) }.flatMap {
        val ints = it.decode(previousLine.get(), previousChar.get());
        if (it.pos.toLine > previousLine.get()) {
            previousLine.set(it.pos.toLine)
        }
        previousChar.set(it.pos.fromColumn)
        return@flatMap ints;
    };
}

private fun modifyerBitflag(vararg modifiers: SemanticTokenModifiers): Int {
    return modifiers.map { it.bitflag() }.reduceOrNull { acc, i -> acc or i } ?: 0
}

internal fun syntaxHighlight(statements: List<N<StatementNode>>): List<Int> {
    return decode(statements.flatMap { it.syntax { it.syntaxTokens() } })
}

private fun token(
    variable: SemanticTokenTypes,
    location: Location,
    vararg modifiers: SemanticTokenModifiers
): SemanticToken {
    return SemanticToken(variable, modifiers, location)
}

private fun <T> N<T>.syntax(function: (T) -> List<SemanticToken>): List<SemanticToken> {
    return when (this) {
        is N.Node -> function(value) + when (val withAttrs = value) {
            is CanHaveAttributes -> withAttrs.attributes.flatMap { it.syntax { syntaxToken(it) } }
            else -> emptyList()
        }
        is N.Error -> listOf(token(COMMENT, node))
    }
}

private fun syntaxToken(it: AttributeNode): List<SemanticToken> {
    return it.name.syntax { listOf(token(MACRO, it.second)) } + it.values.flatMap { it.syntax {
        listOf(
            token(PROPERTY, it.name.location)
        ) + it.value.syntax { it.syntaxTokens() }
    } }
}

private fun StatementNode.syntaxTokens(): List<SemanticToken> {
    return when (this) {
        is StatementNode.AssignmentNode -> target.syntax { it.syntax() } + value.syntax { it.syntaxTokens() }
        is StatementNode.CodeBlockNode.SingleExpressionCodeBlock -> expression.syntax { it.syntaxTokens() }
        is StatementNode.CodeBlockNode.StatementsCodeBlock -> statements.flatMap { it.syntax { it.syntaxTokens() } }
        is StatementNode.FunctionCallNode ->
            listOf(token(FUNCTION, this.function.location { it.location })) +
                    parameters.flatMap { it.syntax { it.syntaxTokens() } }

        is StatementNode.FunctionDeclarationNode ->
            listOfNotNull(
                token(KEYWORD, this.keywordLocation),
                this.name?.location { it.second }?.let { token(FUNCTION, it) }
            ) +
                    parameters.flatMap { it.syntax { it.syntaxTokens() } } +
                    body.syntax { (it as StatementNode).syntaxTokens() } +
                    (returnType?.syntax { it.syntaxTokens() } ?: emptyList())

        is StatementNode.IfNode ->
            listOf(token(KEYWORD, keywordLocation)) +
                    condition.syntax { it.syntaxTokens() } +
                    thenCodeBlockNode.syntax { (it as StatementNode).syntaxTokens() } +
                    (elseStatement?.syntax { it.syntaxTokens() } ?: emptyList())

        is StatementNode.MethodInvocationNode -> method.syntax { it.target.syntax { it.syntaxTokens() } } +
                listOf(token(METHOD, this.method.location { it.field.location })) +
                parameters.flatMap { it.syntax { it.syntaxTokens() } }

        is StatementNode.ReturnNode ->
            listOf(token(KEYWORD, this.keywordLocation)) +
                    (expression?.syntax { it.syntaxTokens() } ?: emptyList())

        is StatementNode.SwitchNode -> listOf(
            token(
                KEYWORD,
                keywordLocation
            )
        ) + item.syntax { it.syntaxTokens() } + cases.flatMap { it.syntax { it.syntaxTokens() } }

        is StatementNode.TypeDefinitionNode ->
            listOf(
                token(KEYWORD, keywordLocation),
                when (val type = type) {
                    is N.Error -> token(COMMENT, type.node)
                    is N.Node -> token(
                        when (type.value) {
                            is TypeNode.ArrayTypeNode -> TYPE
                            is TypeNode.EnumDeclarationNode -> ENUM
                            is TypeNode.FloatTypeNode -> TYPE
                            is TypeNode.IntTypeNode -> TYPE
                            is TypeNode.NamedTypeNode -> TYPE
                            is TypeNode.StringTypeNode -> TYPE
                            is TypeNode.UnionTypeNode -> ENUM
                            is TypeNode.ValueTypeNode -> TYPE
                            is TypeNode.FunctionTypeSignatureNode -> FUNCTION
                            is TypeNode.InterfaceTypeNode -> INTERFACE
                            is TypeNode.VoidTypeNode -> TYPE
                        },
                        type.value.location
                    )
                }
            ) + type.syntax { it.syntaxTokens() }

        is StatementNode.VariableDeclarationNode ->
            listOf(token(KEYWORD, keywordLocation), token(VARIABLE, name.location { it.second })) +
                    (type?.syntax { it.syntaxTokens() } ?: emptyList()) +
                    (value?.syntax { it.syntaxTokens() } ?: emptyList())

        is StatementNode.LineCommentNode -> listOf(token(COMMENT, location))
        is StatementNode.YieldStatement -> listOf(token(KEYWORD, keywordLocation)) + expression.syntax { it.syntaxTokens() }
        is TypeNode.InterfaceTypeNode -> (this as TypeNode).syntaxTokens()
    }
}

private fun StatementNode.AssignmentNode.AssignmentTarget.syntax(): List<SemanticToken> {
    return when (this) {
        is StatementNode.AssignmentNode.AssignmentTarget.FieldTarget -> field.syntax { it.syntaxTokens() }
        is StatementNode.AssignmentNode.AssignmentTarget.IndexAccessTarget -> target.syntax { it.syntaxTokens() }
        is StatementNode.AssignmentNode.AssignmentTarget.VariableTarget -> listOf(token(VARIABLE, location))
    }
}


private fun TypeNode.syntaxTokens(): List<SemanticToken> {
    return when (this) {
        is TypeNode.ArrayTypeNode -> {
            val typeSyntax = this.type.syntax { it.syntaxTokens() }
            typeSyntax + if (this.fixedSize != null)
                listOf(token(NUMBER, this.fixedSize!!.location { it.location }))
            else emptyList()
        }

        is TypeNode.EnumDeclarationNode ->
            listOf(token(KEYWORD, keywordLocation)) +
                    constants.map { token(ENUMMEMBER, it.location { it.location }) }

        is TypeNode.FloatTypeNode,
        is TypeNode.StringTypeNode,
        is TypeNode.IntTypeNode -> listOf(token(TYPE, this.location))

        is TypeNode.NamedTypeNode -> listOf(
            token(
                TYPE,
                this.name.location { it.second }
            )
        ) + genericTypes.flatMap { it.syntax { it.syntaxTokens() } }

        is TypeNode.UnionTypeNode -> types.flatMap { it.syntax { it.syntaxTokens() } }
        is TypeNode.ValueTypeNode -> value.syntax { it.syntaxTokens() }
        is TypeNode.FunctionTypeSignatureNode -> parameters.flatMap { it.syntax { it.syntaxTokens() } } + (returnType?.syntax { it.syntaxTokens() } ?: emptyList())
        is TypeNode.InterfaceTypeNode ->
            functions.flatMap { it.syntax { it.syntaxTokens() } } +
                    (name?.syntax { listOf(token(INTERFACE, it.second)) } ?: emptyList()) +
                    token(KEYWORD, keywordLocation)
        is TypeNode.VoidTypeNode -> listOf(token(KEYWORD, this.location))
    }
}

private fun TypeNode.InterfaceTypeNode.FunctionSignatureNode.syntaxTokens(): List<SemanticToken> {
    return name.syntax { listOf(token(FUNCTION, it.second)) } + typeSignature.syntaxTokens()
}

private fun StatementNode.SwitchNode.CaseNode.syntaxTokens(): List<SemanticToken> {
    return listOf(
        token(KEYWORD, keywordLocation),
    ) + when (val body = body) {
        is CaseBodyNode.CodeBlockCaseBodyNode -> body.codeBlock.syntax { (it as StatementNode).syntaxTokens() }
        is CaseBodyNode.ExpressionCaseBodyNode -> body.expression.syntax { it.syntaxTokens() }
    } + when (val matcher = matcher) {
        is CaseMatchNode.ConditionCaseNode -> matcher.type.syntax { it.syntaxTokens() } + matcher.matching.syntax { it.syntaxTokens() }
        is CaseMatchNode.ConstantCaseNode -> matcher.value.syntax { it.syntaxTokens() }
        is CaseMatchNode.DefaultCaseNode -> listOf(token(KEYWORD, matcher.location))
    }
}

private fun CaseMatchingNode.syntaxTokens(): List<SemanticToken> {
    return when (this) {
        is CaseMatchingNode.CastingPatternMatch -> listOf(
            token(VARIABLE, location)
        )

        is CaseMatchingNode.DeconstructPatternMatch ->
            this.variables.map { token(VARIABLE, it.location) }
    }
}

private fun StatementNode.IfNode.ElseNode?.syntaxTokens(): List<SemanticToken> {
    if (this == null) return emptyList();
    return when (this) {
        is StatementNode.IfNode.ElseNode.ElseBlockNode -> codeBlock.syntax { (it as StatementNode).syntaxTokens() }
        is StatementNode.IfNode.ElseNode.ElseIfNode -> ifNode.syntax { (it as StatementNode).syntaxTokens() }
    } + token(KEYWORD, this.keywordLocation)
}

private fun ParameterNode.syntaxTokens(): List<SemanticToken> {
    return listOf(
        token(
            PARAMETER,
            name.location { it.second }
        )
    ) + type.syntax { it.syntaxTokens() } + (defaultValue?.syntax { it.syntaxTokens() } ?: emptyList())
}

private fun ExpressionNode.syntaxTokens(): List<SemanticToken> {
    return when (this) {
        is ExpressionNode.BooleanLiteralNode -> listOf(token(ENUMMEMBER, location))
        is ExpressionNode.FieldAccessNode -> this.target.syntax { it.syntaxTokens() } + listOf(
            token(
                PROPERTY,
                this.field.location { it.second }
            )
        )

        is ExpressionNode.NumberLiteralNode.FloatLiteralNode -> listOf(token(NUMBER, location))
        is ExpressionNode.NumberLiteralNode.IntegerLiteralNode -> listOf(token(NUMBER, location))
        is ExpressionNode.OperationNode -> left.syntax { it.syntaxTokens() } +
                listOf(token(OPERATOR, operator.second)) +
                right.syntax { it.syntaxTokens() }

        is ExpressionNode.StringLiteralNode -> this.syntaxTokens()
        is ExpressionNode.VariableLiteralNode -> listOf(token(VARIABLE, location))
        is ExpressionNode.IndexAccessNode -> target.syntax { it.syntaxTokens() } + index.syntax { it.syntaxTokens() }
        is StatementNode -> (this as StatementNode).syntaxTokens()
    }
}

private fun ExpressionNode.StringLiteralNode.syntaxTokens(): List<SemanticToken> {
    return content.flatMap {
        it.syntax {
            val symbolismType = MACRO
            when (it) {
                is ExpressionNode.StringLiteralNode.StringPart.Literal -> listOf(
                    token(symbolismType, it.keywordLocation),
                    token(VARIABLE, it.nameLocation)
                )

                is ExpressionNode.StringLiteralNode.StringPart.Expression ->
                    listOf(token(symbolismType, it.startKeywordLocation)) +
                            it.expression.syntax { it.syntaxTokens() } +
                            token(symbolismType, it.endKeywordLocation)

                is ExpressionNode.StringLiteralNode.StringPart.Charsequence -> listOf(token(STRING, it.location))
                is ExpressionNode.StringLiteralNode.StringPart.EscapeSequence -> listOf(
                    token(
                        symbolismType,
                        it.location
                    )
                )
            }
        }
    }
}