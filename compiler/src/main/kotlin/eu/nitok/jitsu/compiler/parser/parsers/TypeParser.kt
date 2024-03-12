package eu.nitok.jitsu.compiler.parser.parsers

import com.niton.jainparse.token.DefaultToken
import com.niton.jainparse.token.TokenStream
import eu.nitok.jitsu.compiler.ast.CompilerMessages
import eu.nitok.jitsu.compiler.ast.StatementNode
import eu.nitok.jitsu.compiler.ast.TypeNode
import eu.nitok.jitsu.compiler.ast.TypeNode.StructuralInterfaceTypeNode.StructuralFieldNode
import eu.nitok.jitsu.compiler.ast.withMessages
import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage
import eu.nitok.jitsu.compiler.model.BitSize
import eu.nitok.jitsu.compiler.model.Visibility
import eu.nitok.jitsu.compiler.parser.*
import kotlin.jvm.optionals.getOrNull


fun parseType(tokens: Tokens): TypeNode? {
    val firstType = parseSingleType(tokens) ?: return null
    val union = parseUnion(firstType, tokens)
    return union ?: firstType
}

fun parseOptionalExplicitType(tokens: Tokens, messages: (CompilerMessage) -> Unit): TypeNode? {
    val colon = tokens.keyword(DefaultToken.COLON) ?: return null
    tokens.skipWhitespace()
    val type = parseType(tokens)
    if (type == null) {
        messages(
            CompilerMessage(
                "Expected type", tokens.location.toRange(), CompilerMessage.Hint("Colon starts type definition", colon)
            )
        )
    }
    return type
}

fun parseTypeDeclaration(tokens: Tokens): StatementNode.NamedTypeDeclarationNode? {
    return parseTypeAlias(tokens)
}

private fun parseTypeAlias(tokens: Tokens): StatementNode.NamedTypeDeclarationNode.TypeAliasNode? {
    val alias = tokens.keyword("type") ?: return null
    tokens.skipWhitespace()
    val messages = CompilerMessages()
    val name = parseIdentifier(tokens)
    if (name == null) {
        messages.error("Expected a name for the type alias", tokens.location.toRange())
    }
    tokens.skipWhitespace()
    tokens.expect(DefaultToken.EQUAL) ?: messages.error(
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
        type,
        alias.rangeTo(tokens.lastConsumedLocation),
        alias,
        listOf()
    )
        .withMessages(messages)
}

fun parseExplicitType(
    tokens: Tokens,
    messages: CompilerMessages
): TypeNode? {
    if (tokens.keyword(DefaultToken.COLON) == null) {
        messages.error("Expected a type definition starting with a ':'", tokens.location.toRange())
        tokens.location
    } else {
        tokens.skipWhitespace()
    }
    val type = parseType(tokens)
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
            if(name == null) {
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
    fields.value?: return null;
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
    parseBitsizedNumberType(tokens)?.let {
        return it
    }

    val structuralInterface = parseStructuralInterface(tokens)
    if (structuralInterface != null) {
        return structuralInterface
    }
    val typeReference = parseIdentifier(tokens) ?: return null
    val namedType = TypeNode.NameTypeNode(typeReference, listOf(), typeReference.location)
    return namedType
}

private fun parseBitsizedNumberType(tokens: TokenStream<DefaultToken>): TypeNode? {
    tokens.elevate()
    val letterToken = tokens.expect(DefaultToken.LETTERS)
    if (letterToken == null) {
        tokens.rollback()
        return null
    }
    val bitSize = {
        tokens.expect(DefaultToken.NUMBER)?.let { numberToken ->
            BitSize.byBits(numberToken.value.value.toInt())?.let { it to numberToken.location }
        }
    }
    val type = when (letterToken.value.value) {
        "i" -> bitSize()?.let { TypeNode.IntTypeNode(it.first, letterToken.location.rangeTo(it.second)) }
        "u" -> bitSize()?.let { TypeNode.UIntTypeNode(it.first, letterToken.location.rangeTo(it.second)) }
        "f" -> bitSize()?.let { TypeNode.FloatTypeNode(it.first, letterToken.location.rangeTo(it.second)) }
        else -> null
    }

    if (type == null) {
        tokens.rollback()
        return null
    }
    tokens.commit()
    return type
}

private fun parseUnion(firstType: TypeNode, tokens: TokenStream<DefaultToken>): TypeNode.UnionTypeNode? {
    if (!tokens.hasNext()) return null
    tokens.elevate()
    tokens.skipWhitespace()
    val (pipe, pipeLocation) = tokens.range { next() }
    if (pipe.type != DefaultToken.PIPE) {
        tokens.rollback()
        return null
    }
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
                    CompilerMessage.Hint("Union starts here", pipeLocation)
                )
            )
        } else {
            types.add(type)
        }
        tokens.skipWhitespace()
        if (tokens.peekOptional().getOrNull()?.type == DefaultToken.PIPE) {
            tokens.next()
        } else {
            break
        }
    }
    return TypeNode.UnionTypeNode(types)
}