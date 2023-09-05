package capabilities

import capabilities.SemanticTokenTypes.*
import eu.nitok.jitsu.compiler.ast.ExpressionNode
import eu.nitok.jitsu.compiler.ast.Location
import eu.nitok.jitsu.compiler.ast.StatementNode
import eu.nitok.jitsu.compiler.ast.StatementNode.FunctionDeclarationNode.ParameterNode
import eu.nitok.jitsu.compiler.ast.TypeNode


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
        return listOf(
            pos.fromLine - previousLine,
            pos.fromColumn - previousChar,
            pos.toColumn - pos.fromColumn,
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
    var previousLine = 1
    var previousChar = 1
    return tokens.flatMap {
        var ints = it.decode(previousLine, previousChar);
        previousLine = it.pos.toLine;
        previousChar = it.pos.toColumn;
        return ints;
    };
}

private fun modifyerBitflag(vararg modifiers: SemanticTokenModifiers): Int {
    return modifiers.map { it.bitflag() }.reduceOrNull { acc, i -> acc or i }?:0
}

internal fun StatementNode.syntaxHighlight(): List<Int> {
    return decode(this.syntaxTokens())
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
            listOf(token(KEYWORD, this.keywordLocation)) +
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
    TODO();
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
