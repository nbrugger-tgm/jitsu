package eu.nitok.jitsu.parser.parsers

import com.niton.jainparse.token.DefaultToken
import com.niton.jainparse.token.DefaultToken.PIPE
import eu.nitok.jitsu.common.BitSize
import eu.nitok.jitsu.common.CompilerMessage
import eu.nitok.jitsu.common.CompilerMessage.Hint
import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.locating.locatedAt
import eu.nitok.jitsu.parser.*
import eu.nitok.jitsu.parser.ast.*
import eu.nitok.jitsu.parser.ast.TypeNode.StructuralInterfaceTypeNode.StructuralFieldNode
import kotlin.jvm.optionals.getOrNull


/**
 * Parses any type expression from the token stream.
 * Handles primitives, named types, arrays, unions, and structural interfaces.
 *
 * Examples: `i32`, `Array<i64>`, `MyType`, `i32 | i64`, `MyType[]`
 *
 * @return The parsed type node, or null if no valid type starts at the current position.
 */
internal fun parseType(tokens: Tokens): TypeNode? {
    var type = parseSingleType(tokens) ?: return null
    while(true) {
        val uberType = parseUnion(type, tokens) ?: parseArrayType(type, tokens)
        if(uberType == null) return type;
        type = uberType
    }
}

/**
 * Parses an array type suffix after a base type has been parsed.
 *
 * Examples: `[]`, `[10]`, `[size * 2]`
 *
 * @param firstType The already-parsed element type.
 * @return An ArrayTypeNode wrapping the element type, or null if no `[` follows.
 */
internal fun parseArrayType(
    firstType: TypeNode,
    tokens: Tokens
): TypeNode? {
    val openBrace = tokens.attempt(DefaultToken.SQUARE_BRACKET_OPEN) ?: return null;
    tokens.skipWhitespace()
    val messages = CompilerMessages()
    val fixedSize = parseExpression(tokens)?.let{ if(it is ExpressionNode.NumberLiteralNode.IntegerLiteralNode) it else {
        messages.error("Currently only number literals are valid compile-time array sizes", it.location)//Use compile time constant resolution when available
        null
    } }

    tokens.skipWhitespace()
    val closeBrace = tokens.attempt(DefaultToken.SQUARE_BRACKET_CLOSED)
    if (closeBrace == null) {
        messages.error(
            "Expected closing ']' for array type", tokens.position.toLocation(),
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
internal fun parseTypeDeclaration(tokens: Tokens, attributes: List<AttributeNode>): StatementNode.NamedTypeDeclarationNode? {
    return parseTypeAlias(tokens, attributes)
}

/**
 * Parses generic type parameter definitions in angle brackets.
 *
 * Examples: `<T>`, `<K, V>`, `<A, B, C>`
 *
 * @return List of type parameter identifiers, or null if no `<` is present.
 */
internal fun parseTypeParameterDefinition(tokens: Tokens, messages: CompilerMessages): List<IdentifierNode>? {
    return tokens.enclosedRepetition(
        DefaultToken.LEFT_ANGLE_BRACKET,
        DefaultToken.COMMA,
        DefaultToken.BIGGER,
        messages,
        "generics",
        "type parameter"
    ) {
        parseIdentifier(it)
    }?.elements
}

private fun parseTypeAlias(tokens: Tokens, attributes: List<AttributeNode>): StatementNode.NamedTypeDeclarationNode.TypeAliasNode? {
    val alias = tokens.keyword("type") ?: return null
    tokens.skipWhitespace()
    val messages = CompilerMessages()
    val name = parseIdentifier(tokens)
    if (name == null) {
        messages.error("Expected a name for the type alias", tokens.position.toLocation())
    }
    val typeParameters = parseTypeParameterDefinition(tokens, messages)
    tokens.skipWhitespace()
    tokens.attempt(DefaultToken.EQUAL) ?: messages.error(
        "Expected '=' after the type alias name",
        tokens.position.toLocation()
    )
    tokens.skipWhitespace()
    val type = parseType(tokens)
    if (type == null) {
        messages.error("Expected a type definition after the '='", tokens.position.toLocation())
    }
    return StatementNode.NamedTypeDeclarationNode.TypeAliasNode(
        name,
        typeParameters ?: listOf(),
        type,
        alias.rangeTo(tokens.lastConsumedLocation),
        alias,
        attributes
    ).withMessages(messages)
}

/**
 * parses explicit type definitions such as ': i64' or ': A | B'
 *
 * @param lenient if `true` the ':' (COLON) is semi-optional => type parsing is still attempted but an error for the missing colon is emitted.
 *                When lenient is `false` parsing is not attempted if no colon is present
 */
internal fun parseExplicitType(
    tokens: Tokens,
    messages: CompilerMessages,
    lenient: Boolean = true,
    explicitTypeRequiredMessage: String? = null
): TypeNode? {
    if (tokens.attempt(DefaultToken.COLON) == null) {
        if(!lenient) return null;
        val type = parseType(tokens) ?: run {
            if(explicitTypeRequiredMessage != null) messages.error(explicitTypeRequiredMessage, tokens.position)
            return null
        }
        messages.error("Expected type definition to starting with a ':'", type.location)
        return type
    }
    tokens.skipWhitespace()
    val type = parseType(tokens)
    if (type == null) {
        messages.error("Expected a type definition after the ':'", tokens.position.toLocation())
    }
    return type
}

private fun parseStructuralInterface(tokens: Tokens): TypeNode? {
    val messages = CompilerMessages()
    val fields = tokens.nullableRange {
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
            tokens.skip(DefaultToken.WHITESPACE)
            return@enclosedRepetition element.withMessages(messages)
        }
    }
    if(fields == null) return null
    val fieldList = fields.value.elements
    return TypeNode.StructuralInterfaceTypeNode(fieldList, fields.location)
        .withMessages(messages)
}

private fun parseSingleType(tokens: Tokens): TypeNode? {
    val messages = CompilerMessages()
    val baseType = tokens.keyword("int")?.let {
        TypeNode.IntTypeNode(BitSize.BIT_32, it)
    }?: tokens.keyword("float")?.let {
        TypeNode.FloatTypeNode(BitSize.BIT_32, it)
    }?: tokens.keyword("boolean")?.let {
        TypeNode.BooleanTypeNode(it)
    }?: parseEnclosedType(tokens, messages)?.let {
         it
    }?: parseBitsizedNumberType(tokens)?.let {
        it
    }?: parseStructuralInterface(tokens)?.let {
        it
    }?: run {
        val typeReference = parseIdentifier(tokens) ?: return null
        val generics = tokens.enclosedRepetition(
            DefaultToken.LEFT_ANGLE_BRACKET,
            DefaultToken.COMMA,
            DefaultToken.BIGGER,
            messages,
            "generics",
            "type parameter"
        ) {
            parseType(it)
        }?.elements ?: listOf()
        TypeNode.NameTypeNode(typeReference, generics, typeReference.location).withMessages(messages)
    }
    return tokens.attempt(DefaultToken.QUESTIONMARK)?.let {
        TypeNode.UnionTypeNode(listOf(baseType, TypeNode.NullTypeNode(it.location)))
    }?: baseType;
}

private fun parseEnclosedType(tokens: Tokens, messages: CompilerMessages): TypeNode? {
    val type = tokens.attempt {
        val openKw = tokens.attempt(DefaultToken.BRACKET_OPEN)?: return@attempt null
        tokens.skipWhitespace()
        parseType(tokens)?.let { it to openKw }
    }?: return null
    tokens.skipWhitespace()
    tokens.attempt(DefaultToken.BRACKET_CLOSED) ?: run {
        messages.error("Expected closing ')'", tokens.position.toLocation(), Hint("Type grouping started here",type.second.location))
    }
    return type.first.withMessages(messages)
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

private fun parseUnion(firstType: TypeNode, tokens: Tokens): TypeNode.UnionTypeNode? {
    if (!tokens.hasNext()) return null
    tokens.skipWhitespace()
    val pipe = tokens.attempt {
        skipWhitespace()
        range { next().type == PIPE }.takeIf { it.value }
    } ?: return null
    val types = mutableListOf(firstType)
    val messages = CompilerMessages()
    while (tokens.hasNext()) {
        tokens.skipWhitespace()
        val type = parseSingleType(tokens)
        if (type == null) {
            messages.error(
                CompilerMessage(
                    "Expect type for union",
                    tokens.position.toLocation(),
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
