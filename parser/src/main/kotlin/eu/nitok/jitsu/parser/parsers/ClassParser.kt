package eu.nitok.jitsu.parser.parsers

import com.niton.jainparse.token.DefaultToken
import com.niton.jainparse.token.DefaultToken.*
import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.parser.*
import eu.nitok.jitsu.parser.ast.AttributeNode
import eu.nitok.jitsu.parser.ast.StatementNode.NamedTypeDeclarationNode.ClassDeclarationNode
import eu.nitok.jitsu.parser.ast.TypeNode.StructuralInterfaceTypeNode.StructuralFieldNode
import eu.nitok.jitsu.parser.ast.withMessages

/**
 * Parses a class declaration starting with the `class` keyword.
 *
 * Syntax: `class <name>[<generics>] { <fields and methods> }`
 *
 * Examples:
 * - `class Point { x: i32; y: i32; }`
 * - `class Box<T> { value: T; }`
 * - `class Counter { mut count: i32; fn increment() {} }`
 *
 * @return A ClassDeclarationNode, or null if no `class` keyword is present.
 */
internal fun parseClass(tokens: Tokens, attributes: List<AttributeNode>): ClassDeclarationNode? {
    val classToken = tokens.keyword("class") ?: return null
    tokens.skipWhitespace()
    val name = parseIdentifier(tokens)
    tokens.skipWhitespace()
    val messages = CompilerMessages()
    val typeParameters = parseTypeParameterDefinition(tokens, messages)
    tokens.skipWhitespace()
    val bodyStart = tokens.attempt(ROUND_BRACKET_OPEN);
    bodyStart ?: messages.error(
        "Expected '{' after class declaration",
        tokens.position.toLocation()
    )
    val fields = mutableListOf<StructuralFieldNode>()
    val methods = mutableListOf<ClassDeclarationNode.MethodNode>()
    var closed = false
    while (tokens.hasNext()) {
        tokens.skipWhitespace()
        if (tokens.peek().type == ROUND_BRACKET_CLOSED) {
            tokens.skip()
            closed = true
            break
        }
        val attributes = parseAttributes(tokens)
        val method = parseMethod(tokens, attributes)
        if (method != null) {
            methods.add(method)
            continue
        }
        val field = parseField(tokens, attributes)
        if (field != null) {
            fields.add(field)
            continue
        }
        val invalid = tokens.skipUntil(ROUND_BRACKET_CLOSED, NEW_LINE, EOF)
        messages.error("Expect field or method declaration", invalid)
    }
    if(bodyStart != null && !closed) {
        messages.error("Expected '}' to close class body", tokens.position.toLocation())
    }
    return ClassDeclarationNode(
        name,
        typeParameters?:listOf(),
        fields,
        methods,
        classToken.rangeTo(tokens.lastConsumedLocation),
        classToken,
        attributes
    ).withMessages(messages)
}

/**
 * Parses a field declaration within a class body.
 *
 * Syntax: `[public] [mut] <name>: <type>;`
 *
 * Examples: `x: i32;`, `public mut count: i64;`
 *
 * @return A StructuralFieldNode, or null if no valid field declaration is present.
 */
internal fun parseField(tokens: Tokens, attributes: List<AttributeNode>): StructuralFieldNode? {
    tokens.elevate()
    val publicKw = tokens.keyword("public");
    if(publicKw != null) tokens.skipWhitespace()
    val mut = tokens.keyword("mut");
    if(mut != null) tokens.skipWhitespace()
    val name = parseIdentifier(tokens)
    if (name == null) {
        tokens.rollback()
        return null
    } else tokens.commit()

    tokens.skipWhitespace()
    val messages = CompilerMessages()
    val type = parseExplicitType(tokens, messages, explicitTypeRequiredMessage = "Class fields require type declaration ': fieldtype'")
    tokens.skipWhitespace()
    tokens.attempt(SEMICOLON) ?: messages.error(
        "Expected ';' after field declaration",
        tokens.position.toLocation()
    )
    return StructuralFieldNode(
        name,
        type,
        mut,
        publicKw?.let { Located("public", it) }
    ).withMessages(messages)
}

/**
 * Parses a method declaration within a class body.
 *
 * Syntax: `[mut] fn <name>([params])[: returnType] { body }`
 *
 * @return A MethodNode, or null if no valid method declaration is present.
 */
internal fun parseMethod(tokens: Tokens, attributes: List<AttributeNode>): ClassDeclarationNode.MethodNode? {
    tokens.elevate()
    val mutable = tokens.keyword("mut")
    if(mutable != null) tokens.skipWhitespace()
    val func = parseFunction(tokens, attributes)
    if (func == null) {
        tokens.rollback();
        return null
    }
    tokens.commit();
    return ClassDeclarationNode.MethodNode(func, mutable)
}
