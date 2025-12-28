package eu.nitok.jitsu.compiler.parser.parsers

import com.niton.jainparse.token.DefaultToken
import com.niton.jainparse.token.DefaultToken.PIPE
import com.niton.jainparse.token.TokenStream
import eu.nitok.jitsu.compiler.ast.CompilerMessages
import eu.nitok.jitsu.compiler.ast.IdentifierNode
import eu.nitok.jitsu.compiler.ast.StatementNode
import eu.nitok.jitsu.compiler.ast.TypeNode
import eu.nitok.jitsu.compiler.ast.TypeNode.StructuralInterfaceTypeNode.StructuralFieldNode
import eu.nitok.jitsu.compiler.ast.locatedAt
import eu.nitok.jitsu.compiler.ast.withMessages
import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage
import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage.Hint
import eu.nitok.jitsu.compiler.model.BitSize
import eu.nitok.jitsu.compiler.parser.*
import kotlin.jvm.optionals.getOrNull


fun parseType(tokens: Tokens): TypeNode? {
    var type = parseSingleType(tokens) ?: return null
    while(true) {
        val uberType = parseUnion(type, tokens) ?: parseArrayType(type, tokens)
        if(uberType == null) return type;
        type = uberType
    }
}

fun parseArrayType(
    firstType: TypeNode,
    tokens: Tokens
): TypeNode? {
    val openBrace = tokens.attempt(DefaultToken.SQUARE_BRACKET_OPEN) ?: return null;
    tokens.skipWhitespace()
    val messages = CompilerMessages()
    val fixedSize = parseExpression(tokens)

    tokens.skipWhitespace()
    val closeBrace = tokens.attempt(DefaultToken.SQUARE_BRACKET_CLOSED)
    if (closeBrace == null) {
        messages.error(
            "Expected closing ']' for array type", tokens.location.toRange(),
            Hint("Opening ']'", openBrace.location)
        )
    }
    return TypeNode.ArrayTypeNode(firstType, fixedSize, firstType.location.rangeTo((closeBrace ?: openBrace).location))
        .withMessages(messages)
}

/**
 * parses constructs such as
 *
 * type X = A | B
 * type Y = { test: X }
 *
 * general syntax:
 * type <identifier> = <any type>
 */
fun parseTypeDeclaration(tokens: Tokens): StatementNode.NamedTypeDeclarationNode? {
    return parseTypeAlias(tokens)
}

fun parseTypeParameterDefinition(tokens: Tokens, messages: CompilerMessages): List<IdentifierNode>? {
    return tokens.enclosedRepetition(
        DefaultToken.LEFT_ANGLE_BRACKET,
        DefaultToken.COMMA,
        DefaultToken.BIGGER,
        messages,
        "generics",
        "type parameter"
    ) {
        parseIdentifier(it)
    }
}

private fun parseTypeAlias(tokens: Tokens): StatementNode.NamedTypeDeclarationNode.TypeAliasNode? {
    val alias = tokens.keyword("type") ?: return null
    tokens.skipWhitespace()
    val messages = CompilerMessages()
    val name = parseIdentifier(tokens)
    if (name == null) {
        messages.error("Expected a name for the type alias", tokens.location.toRange())
    }
    val typeParameters = parseTypeParameterDefinition(tokens, messages)
    tokens.skipWhitespace()
    tokens.attempt(DefaultToken.EQUAL) ?: messages.error(
        "Expected '=' after the type alias name",
        tokens.location.toRange()
    )
    tokens.skipWhitespace()
    val type = parseType(tokens)
    if (type == null) {
        messages.error("Expected a type definition after the '='", tokens.location.toRange())
    }
    return StatementNode.NamedTypeDeclarationNode.TypeAliasNode(
        name,
        typeParameters ?: listOf(),
        type,
        alias.rangeTo(tokens.lastConsumedLocation),
        alias,
        listOf()
    ).withMessages(messages)
}

fun parseExplicitType(
    tokens: Tokens,
    messages: CompilerMessages
): TypeNode? {
    if (tokens.attempt(DefaultToken.COLON) == null) {
        val type = parseType(tokens) ?: return null
        messages.error("Expected a type definition starting with a ':'", type.location)
        return type
    }
    tokens.skipWhitespace()
    val type = parseType(tokens)
    if (type == null) {
        messages.error("Expected a type definition after the ':'", tokens.location.toRange())
    }
    return type
}

private fun parseStructuralInterface(tokens: Tokens): TypeNode? {
    val messages = CompilerMessages()
    val fields = tokens.range {
        enclosedRepetition(
            DefaultToken.ROUND_BRACKET_OPEN,
            DefaultToken.COMMA,
            DefaultToken.ROUND_BRACKET_CLOSED,
            messages,
            "interface",
            "field"
        ) {
            tokens.elevate()
            val mut = tokens.keyword("mut")
            tokens.skipWhitespace()
            val name = parseIdentifier(tokens);
            if (name == null) {
                tokens.rollback()
                return@enclosedRepetition null
            } else tokens.commit()
            tokens.skip(DefaultToken.WHITESPACE)
            val messages = CompilerMessages()
            val type = parseExplicitType(tokens, messages)
            val element = StructuralFieldNode(name, type, mut, null)
            messages.apply(element)
            tokens.skip(DefaultToken.WHITESPACE)
            return@enclosedRepetition element
        }
    }
    fields.value ?: return null;
    return TypeNode.StructuralInterfaceTypeNode(fields.value, fields.location)
        .withMessages(messages)
}

private fun parseSingleType(tokens: Tokens): TypeNode? {
    tokens.keyword("int")?.let {
        return TypeNode.IntTypeNode(BitSize.BIT_32, it)
    }
    tokens.keyword("void")?.let {
        return TypeNode.VoidTypeNode(it)
    }
    tokens.keyword("float")?.let {
        return TypeNode.FloatTypeNode(BitSize.BIT_32, it)
    }
    tokens.keyword("boolean")?.let {
        return TypeNode.BooleanTypeNode(it)
    }
    parseBitsizedNumberType(tokens)?.let {
        return it
    }

    val structuralInterface = parseStructuralInterface(tokens)
    if (structuralInterface != null) {
        return structuralInterface
    }
    val typeReference = parseIdentifier(tokens) ?: return null
    val messages = CompilerMessages()
    var generics = tokens.enclosedRepetition(
        DefaultToken.LEFT_ANGLE_BRACKET,
        DefaultToken.COMMA,
        DefaultToken.BIGGER,
        messages,
        "generics",
        "type parameter"
    ) {
        parseType(it)
    } ?: listOf()
    val namedType = TypeNode.NameTypeNode(typeReference, generics, typeReference.location)
    return namedType.withMessages(messages)
}

private fun parseBitsizedNumberType(tokens: Tokens): TypeNode? {
    return tokens.attempt {
        val letterToken = tokens.attempt(DefaultToken.LETTERS) ?: return@attempt null
        val bitSize = {
            attempt(DefaultToken.NUMBER)?.let { numberToken ->
                BitSize.byBits(numberToken.value.value.toInt())?.locatedAt(numberToken.location)
            }
        }
        val type = when (letterToken.value.value) {
            "i" -> bitSize()?.let { TypeNode.IntTypeNode(it.value, letterToken.location.rangeTo(it.location)) }
            "u" -> bitSize()?.let { TypeNode.UIntTypeNode(it.value, letterToken.location.rangeTo(it.location)) }
            "f" -> bitSize()?.let { TypeNode.FloatTypeNode(it.value, letterToken.location.rangeTo(it.location)) }
            else -> null
        }
        type
    }
}

private fun parseUnion(firstType: TypeNode, tokens: TokenStream<DefaultToken>): TypeNode.UnionTypeNode? {
    if (!tokens.hasNext()) return null
    tokens.skipWhitespace()
    val pipe = (tokens.attempt {
        skipWhitespace()
        range { next().type == PIPE }.takeIf { it.value }
    }) ?: return null
    val types = mutableListOf(firstType)
    val messages = CompilerMessages()
    while (tokens.hasNext()) {
        tokens.skipWhitespace()
        val type = parseSingleType(tokens)
        if (type == null) {
            messages.error(
                CompilerMessage(
                    "Expect type for union",
                    tokens.location.toRange(),
                    Hint("Union starts here", pipe.location)
                )
            )
        } else {
            types.add(type)
        }
        tokens.skipWhitespace()
        if (tokens.peekOptional().getOrNull()?.type == PIPE) {
            tokens.next()
        } else {
            break
        }
    }
    return TypeNode.UnionTypeNode(types).withMessages(messages)
}