package capabilities

import capabilities.SemanticTokenTypes.*
import eu.nitok.jitsu.common.flatMap
import eu.nitok.jitsu.common.locating.Location
import eu.nitok.jitsu.parser.ast.*
import eu.nitok.jitsu.parser.ast.ExpressionNode.*
import eu.nitok.jitsu.parser.ast.StatementNode.Declaration.FunctionDeclarationNode
import eu.nitok.jitsu.parser.ast.StatementNode.InstructionNode.*
import eu.nitok.jitsu.parser.ast.StatementNode.InstructionNode.SwitchNode.CaseNode
import eu.nitok.jitsu.parser.ast.StatementNode.NamedTypeDeclarationNode
import eu.nitok.jitsu.parser.ast.StatementNode.NamedTypeDeclarationNode.EnumDeclarationNode
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
        val lineDiff = pos.start.line - previousLine
        return listOf(
            lineDiff,
            if (lineDiff > 0) pos.start.column - 1 else pos.start.column - previousChar,
            pos.end.column - pos.start.column + 1,
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
        if (it.pos.end.line > previousLine.get()) {
            previousLine.set(it.pos.end.line)
        }
        previousChar.set(it.pos.start.column)
        return@flatMap ints;
    }
}

private fun modifyerBitflag(vararg modifiers: SemanticTokenModifiers): Int {
    return modifiers.map { it.bitflag() }.reduceOrNull { acc, i -> acc or i } ?: 0
}

internal fun syntaxHighlight(statements: SourceFileNode): List<Int> {
    return decode(statements.statements.flatMap {
        val semanticTokens = it.flatMap { it.syntaxTokens() }
        semanticTokens
    })
}

private fun token(
    type: SemanticTokenTypes,
    location: Location,
    vararg modifiers: SemanticTokenModifiers
): SemanticToken {
    return SemanticToken(type, modifiers, location)
}

internal val symbolismType = MACRO

private fun AstNode.syntaxTokens(): List<SemanticToken> {
    return when (this) {
        is FunctionCallNode -> listOf(token(FUNCTION, function.location))
        is FunctionDeclarationNode -> listOfNotNull(
            token(KEYWORD, keywordLocation),
            name?.let { token(FUNCTION, it.location) }
        )

        is IfNode -> listOf(token(KEYWORD, keywordLocation))
        is MethodInvocationNode -> listOf(token(METHOD, method.location))
        is ReturnNode -> listOf(token(KEYWORD, keywordLocation))
        is SwitchNode -> listOf(token(KEYWORD, keywordLocation))
        is NamedTypeDeclarationNode.TypeAliasNode ->
            listOfNotNull(
                token(KEYWORD, keywordLocation),
                name?.let {
                    token(
                        when (type) {
                            is TypeNode.ArrayTypeNode,
                            is TypeNode.FloatTypeNode,
                            is TypeNode.IntTypeNode,
                            is TypeNode.NameTypeNode,
                            is TypeNode.UIntTypeNode,
                            is TypeNode.BooleanTypeNode,
                            is TypeNode.NullTypeNode,
                            is TypeNode.ValueTypeNode -> TYPE

                            is TypeNode.UnionTypeNode -> ENUM
                            is TypeNode.FunctionTypeSignatureNode -> FUNCTION
                            is TypeNode.StructuralInterfaceTypeNode -> INTERFACE
                            null -> TYPE
                        }, it.location
                    )
                }
            )

        is VariableDeclarationNode -> listOfNotNull(
            token(KEYWORD, keywordLocation),
            name?.location?.let { token(VARIABLE, it) }
        )

        is LineCommentNode -> listOf(token(COMMENT, location))
        is YieldStatement -> listOf(token(KEYWORD, keywordLocation))
        is VariableReferenceNode -> listOf(token(VARIABLE, location))
        is EnumDeclarationNode -> listOf(token(KEYWORD, keywordLocation)) +
                constants.map { token(ENUMMEMBER, it.location) }

        is TypeNode.FloatTypeNode,
        is TypeNode.IntTypeNode,
        is TypeNode.UIntTypeNode,
        is BooleanLiteralNode -> listOf(token(KEYWORD, location))

        is NamedTypeDeclarationNode.ClassDeclarationNode -> listOfNotNull(
            name?.location?.let { token(CLASS, it) },
            token(KEYWORD, this.keywordLocation)
        )

        is TypeNode.NameTypeNode -> listOf(token(TYPE, name.location))
        is NamedTypeDeclarationNode.InterfaceTypeNode -> listOf(
            token(INTERFACE, name.location),
            token(KEYWORD, keywordLocation)
        )

        is NamedTypeDeclarationNode.InterfaceTypeNode.FunctionSignatureNode -> listOf(token(FUNCTION, name.location))
        is CaseNode.CaseMatchNode.DefaultCaseNode -> listOf(token(KEYWORD, location))
        is CaseNode.CaseMatchNode.ConstantCaseNode -> listOf(token(KEYWORD, keywordLocation))
        is CaseNode.CaseMatchNode.TypeCaseNode -> listOf(token(KEYWORD, keywordLocation))
        is CaseNode -> listOf(token(KEYWORD, keywordLocation))
        is IfNode.ElseNode -> listOf(token(KEYWORD, keywordLocation))
        is FunctionDeclarationNode.ParameterNode -> listOf(token(PARAMETER, name.location))
        is FieldAccessNode -> this.field?.let { listOf(token(PROPERTY, it.location)) } ?: listOf()
        is NumberLiteralNode -> listOf(token(NUMBER, location))
        is OperationNode -> listOf(token(OPERATOR, operator.location))
        is StringLiteralNode.StringPart.VarReference -> listOfNotNull(
            token(symbolismType, keywordLocation),
            literal?.location?.let { token(VARIABLE, it) }
        )
        is StringLiteralNode.StringPart.Expression -> listOfNotNull(
            token(symbolismType, startKeywordLocation),
            endKeywordLocation?.let { token(symbolismType, it) }
        )
        is StringLiteralNode.StringPart.CharSequence -> listOf(token(STRING, location))
        is StringLiteralNode.StringPart.EscapeSequence -> listOf(token(symbolismType, location))
        is TypeNode.StructuralInterfaceTypeNode.StructuralFieldNode -> listOfNotNull(
            this.visibility?.let { token(KEYWORD, it.location) },
            this.mutableKw?.let { token(KEYWORD, it) },
            token(PROPERTY, name.location)
        )

        else -> listOf()
    }
}