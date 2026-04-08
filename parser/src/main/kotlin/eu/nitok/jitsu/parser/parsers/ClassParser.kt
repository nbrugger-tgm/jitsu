package eu.nitok.jitsu.parser.parsers

import com.niton.jainparse.token.DefaultToken
import com.niton.jainparse.token.DefaultToken.EOF
import com.niton.jainparse.token.DefaultToken.NEW_LINE
import com.niton.jainparse.token.DefaultToken.ROUND_BRACKET_CLOSED
import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.Located
import eu.nitok.jitsu.parser.ast.StatementNode.NamedTypeDeclarationNode.ClassDeclarationNode
import eu.nitok.jitsu.parser.ast.TypeNode.StructuralInterfaceTypeNode.StructuralFieldNode
import eu.nitok.jitsu.parser.ast.withMessages
import eu.nitok.jitsu.parser.*

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
fun parseClass(tokens: Tokens): ClassDeclarationNode? {
    val classToken = tokens.keyword("class") ?: return null
    tokens.skipWhitespace()
    val name = parseIdentifier(tokens)
    tokens.skipWhitespace()
    val messages = CompilerMessages()
    val typeParameters = parseTypeParameterDefinition(tokens, messages)
    tokens.skipWhitespace()
    val bodyStart = tokens.attempt(DefaultToken.ROUND_BRACKET_OPEN);
    bodyStart ?: messages.error(
        "Expected '{' after class declaration",
        tokens.location.toRange()
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
        val invalid = tokens.skipUntil(ROUND_BRACKET_CLOSED, NEW_LINE, EOF)
        messages.error("Expect field or method declaration", invalid)
    }
    if(bodyStart != null && !closed) {
        messages.error("Expected '}' to close class body", tokens.location.toRange())
    }
    return ClassDeclarationNode(
        name,
        typeParameters?:listOf(),
        fields,
        methods,
        classToken.rangeTo(tokens.lastConsumedLocation),
        classToken,
        listOf()
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
fun parseField(tokens: Tokens): StructuralFieldNode? {
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
    val type = parseExplicitType(tokens, messages)
    if(type == null) {
        messages.error("Class fields require type declaration ': fieldtype'",tokens.location.toRange())
    }
    tokens.skipWhitespace()
    tokens.attempt(DefaultToken.SEMICOLON) ?: messages.error(
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

/**
 * Parses a method declaration within a class body.
 *
 * Syntax: `[mut] fn <name>([params])[: returnType] { body }`
 *
 * @return A MethodNode, or null if no valid method declaration is present.
 */
fun parseMethod(tokens: Tokens): ClassDeclarationNode.MethodNode? {
    tokens.elevate()
    val mutable = tokens.keyword("mut")
    if(mutable != null) tokens.skipWhitespace()
    val func = parseFunction(tokens)
    if (func == null) {
        tokens.rollback();
        return null
    }
    tokens.commit();
    return ClassDeclarationNode.MethodNode(func, mutable)
}
