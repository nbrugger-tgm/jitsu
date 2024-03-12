package eu.nitok.jitsu.compiler.parser.parsers

import com.niton.jainparse.token.DefaultToken
import eu.nitok.jitsu.compiler.ast.CompilerMessages
import eu.nitok.jitsu.compiler.ast.IdentifierNode
import eu.nitok.jitsu.compiler.ast.Located
import eu.nitok.jitsu.compiler.ast.StatementNode.NamedTypeDeclarationNode.ClassDeclarationNode
import eu.nitok.jitsu.compiler.ast.TypeNode.StructuralInterfaceTypeNode.StructuralFieldNode
import eu.nitok.jitsu.compiler.ast.withMessages
import eu.nitok.jitsu.compiler.model.Visibility
import eu.nitok.jitsu.compiler.parser.*

fun parseClass(tokens: Tokens): ClassDeclarationNode? {
    val classToken = tokens.keyword("class") ?: return null
    tokens.skipWhitespace()
    val name = parseIdentifier(tokens)
    tokens.skipWhitespace()
    val messages = CompilerMessages()
    val typeParameters = parseTypeParameters(tokens, messages)
    tokens.skipWhitespace()
    tokens.expect(DefaultToken.ROUND_BRACKET_OPEN) ?: messages.error(
        "Expected '{' after class declaration",
        tokens.location.toRange()
    )
    val fields = mutableListOf<StructuralFieldNode>()
    val methods = mutableListOf<ClassDeclarationNode.MethodNode>()
    while (tokens.hasNext()) {
        tokens.skipWhitespace()
        if (tokens.peek().type == DefaultToken.ROUND_BRACKET_CLOSED) {
            tokens.skip()
            break
        }
        val method = parseMethod(tokens)
        if (method != null) {
            methods.add(method)
            continue
        }
        val field = parseField(tokens)
        if (field != null) {
            fields.add(field)
            continue
        }
        var invalid = tokens.skipUntil(DefaultToken.ROUND_BRACKET_CLOSED, DefaultToken.NEW_LINE)
        messages.error("Expect field or method declaration", invalid)
        tokens.expect(DefaultToken.ROUND_BRACKET_CLOSED) ?: messages.error(
            "Expected '}' to close class body",
            tokens.location.toRange()
        )
        break
    }
    return ClassDeclarationNode(
        name,
        typeParameters,
        fields,
        methods,
        classToken.rangeTo(tokens.lastConsumedLocation),
        classToken,
        listOf()
    ).withMessages(messages)
}

fun parseTypeParameters(tokens: Tokens, messages: CompilerMessages): List<IdentifierNode> {
    return tokens.enclosedRepetition(
        DefaultToken.LEFT_ANGLE_BRACKET,
        DefaultToken.COMMA,
        DefaultToken.BIGGER,
        messages,
        "generics",
        "type parameter"
    ) {
        parseIdentifier(it)
    } ?: listOf()
}

fun parseField(tokens: Tokens): StructuralFieldNode? {
    tokens.elevate()
    val publicKw = tokens.keyword("public");
    tokens.skipWhitespace()
    val mut = tokens.keyword("mut");
    tokens.skipWhitespace()
    val name = parseIdentifier(tokens)
    if (name == null) {
        tokens.rollback()
        return null
    } else tokens.commit()

    tokens.skipWhitespace()
    val messages = CompilerMessages()
    val type = parseExplicitType(tokens, messages)
    tokens.skipWhitespace()
    tokens.expect(DefaultToken.SEMICOLON) ?: messages.error(
        "Expected ';' after field declaration",
        tokens.location.toRange()
    )
    return StructuralFieldNode(
        name,
        type,
        mut,
        publicKw?.let { Located("public", it) }
    ).withMessages(messages)
}

fun parseMethod(tokens: Tokens): ClassDeclarationNode.MethodNode? {
    tokens.elevate()
    val mutable = tokens.keyword("mut")
    val func = parseFunction(tokens)
    if (func == null) {
        tokens.rollback();
        return null
    }
    tokens.commit();
    return ClassDeclarationNode.MethodNode(func, mutable)
}