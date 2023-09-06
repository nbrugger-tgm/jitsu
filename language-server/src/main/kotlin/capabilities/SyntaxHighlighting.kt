package capabilities

import capabilities.SemanticTokenTypes.*
import customLogger
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
        customLogger.println("previousLine: $previousLine, previousChar: $previousChar")
        customLogger.println("abs start line: ${pos.fromLine}, abs start char: ${pos.fromColumn}")
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
    return tokens.flatMap {
        customLogger.println("decoding $it")
        customLogger.println("previousLine: $previousLine, previousChar: $previousChar")
        val ints = it.decode(previousLine.get(), previousChar.get());
        customLogger.println(" -> $ints")
        customLogger.flush()
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

internal fun syntaxHighlight(statements: List<StatementNode>): List<Int> {
    return decode(statements.flatMap { it.syntaxTokens() })
}

private fun StatementNode.syntaxTokens(): List<SemanticToken> {
    return when (this) {
        is StatementNode.AssignmentNode -> listOf(token(VARIABLE, nameLocation)) + value.syntaxTokens()
        is StatementNode.CodeBlockNode.SingleExpressionCodeBlock -> expression.syntaxTokens()
        is StatementNode.CodeBlockNode.StatementsCodeBlock -> statements.flatMap { it.syntaxTokens() }
        is StatementNode.FunctionCallNode ->
            listOf(token(FUNCTION, nameLocation)) +
                    parameters.flatMap { it.syntaxTokens() }

        is StatementNode.FunctionDeclarationNode ->
            listOf(token(FUNCTION, this.nameLocation)) +
                    parameters.flatMap { it.syntaxTokens() } +
                    body.syntaxTokens()

        is StatementNode.IfNode ->
            listOf(token(KEYWORD, keywordLocation)) +
                    condition.syntaxTokens() +
                    thenCodeBlockNode.syntaxTokens() +
                    elseStatement.syntaxTokens()

        is StatementNode.MethodInvocationNode -> target.syntaxTokens() +
                listOf(token(METHOD, nameLocation)) +
                parameters.flatMap { it.syntaxTokens() }

        is StatementNode.ReturnNode ->
            listOf(token(KEYWORD, this.keywordLocation)) +
                    (expression?.syntaxTokens() ?: emptyList())

        is StatementNode.SwitchNode -> listOf(
            token(
                KEYWORD,
                keywordLocation
            )
        ) + item.syntaxTokens() + cases.flatMap { it.syntaxTokens() }

        is StatementNode.TypeDefinitionNode ->
            listOf(
                token(KEYWORD, keywordLocation), token(
                    when (type) {
                        is TypeNode.ArrayTypeNode -> TYPE
                        is TypeNode.EnumDeclarationNode -> ENUM
                        is TypeNode.FloatTypeNode -> TYPE
                        is TypeNode.IntTypeNode -> TYPE
                        is TypeNode.NamedTypeNode -> TYPE
                        is TypeNode.StringTypeNode -> TYPE
                        is TypeNode.UnionTypeNode -> ENUM
                        is TypeNode.ValueTypeNode -> TYPE
                    }, nameLocation
                )
            ) + type.syntaxTokens()

        is StatementNode.VariableDeclarationNode ->
            listOf(token(KEYWORD, keywordLocation), token(VARIABLE, nameLocation)) +
                    (type?.syntaxTokens() ?: emptyList()) +
                    (value?.syntaxTokens() ?: emptyList())
    }
}

private fun TypeNode.syntaxTokens(): List<SemanticToken> {
    return when (this) {
        is TypeNode.ArrayTypeNode ->
            this.type.syntaxTokens() +
                    if (this.sizeLocation != null && this.fixedSize != null)
                        listOf(token(NUMBER, this.sizeLocation!!))
                    else emptyList()

        is TypeNode.EnumDeclarationNode ->
            listOf(token(KEYWORD, keywordLocation)) +
                    constants.map { token(ENUMMEMBER, it.location) }

        is TypeNode.FloatTypeNode,
        is TypeNode.StringTypeNode,
        is TypeNode.IntTypeNode -> listOf(token(KEYWORD, this.location))

        is TypeNode.NamedTypeNode -> listOf(token(TYPE, this.nameLocation)) + genericTypes.flatMap { it.syntaxTokens() }
        is TypeNode.UnionTypeNode -> types.flatMap { it.syntaxTokens() }
        is TypeNode.ValueTypeNode -> value.syntaxTokens()
    }
}

private fun StatementNode.SwitchNode.CaseNode.syntaxTokens(): List<SemanticToken> {
    return listOf(
        token(KEYWORD, keywordLocation),
    ) + when(val body = body) {
        is CaseBodyNode.CodeBlockCaseBodyNode -> body.codeBlock.syntaxTokens()
        is CaseBodyNode.ExpressionCaseBodyNode -> body.expression.syntaxTokens()
    } + when(val matcher = matcher){
        is CaseMatchNode.ConditionCaseNode -> matcher.type.syntaxTokens() + matcher.matching.syntaxTokens()
        is CaseMatchNode.ConstantCaseNode -> matcher.value.syntaxTokens()
        is CaseMatchNode.DefaultCaseNode -> listOf(token(KEYWORD, matcher.location))
    }
}

private fun CaseMatchingNode.syntaxTokens(): List<SemanticToken> {
    return when(this) {
        is CaseMatchingNode.CastingPatternMatch -> listOf(
            token(VARIABLE, location)
        )
        is CaseMatchingNode.DeconstructPatternMatch ->
            this.variables.map { token(VARIABLE, it.location) }
    }
}

private fun StatementNode.IfNode.ElseNode?.syntaxTokens(): List<SemanticToken> {
    return when (this) {
        is StatementNode.IfNode.ElseNode.ElseBlockNode -> codeBlock.syntaxTokens()
        is StatementNode.IfNode.ElseNode.ElseIfNode -> ifNode.syntaxTokens()
        null -> emptyList()
    }
}

private fun ParameterNode.syntaxTokens(): List<SemanticToken> {
    return listOf(token(PARAMETER, nameLocation)) + type.syntaxTokens() + (defaultValue?.syntaxTokens() ?: emptyList())
}

private fun ExpressionNode.syntaxTokens(): List<SemanticToken> {
    return when (this) {
        is ExpressionNode.BooleanLiteralNode -> listOf(token(ENUMMEMBER, location))
        is ExpressionNode.FieldAccessNode -> this.target.syntaxTokens() + listOf(token(PROPERTY, this.fieldLocation))
        is ExpressionNode.NumberLiteralNode.FloatLiteralNode -> listOf(token(NUMBER, location))
        is ExpressionNode.NumberLiteralNode.IntegerLiteralNode -> listOf(token(NUMBER, location))
        is ExpressionNode.OperationNode -> left.syntaxTokens() +
                listOf(token(OPERATOR, operatorLocation)) +
                right.syntaxTokens()

        is ExpressionNode.StatementExpressionNode -> statement.syntaxTokens()
        is ExpressionNode.StringLiteralNode -> listOf(token(STRING, location))
        is ExpressionNode.VariableLiteralNode -> listOf(token(VARIABLE, location))
    }
}

private fun token(
    variable: SemanticTokenTypes,
    location: Location,
    vararg modifiers: SemanticTokenModifiers
): SemanticToken {
    return SemanticToken(variable, modifiers, location)
}
