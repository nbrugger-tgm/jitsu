package eu.nitok.jitsu.compiler.parser.parsers

import com.niton.jainparse.token.DefaultToken
import eu.nitok.jitsu.compiler.ast.CompilerMessages
import eu.nitok.jitsu.compiler.ast.TypeNode
import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage
import eu.nitok.jitsu.compiler.parser.*


fun parseStructuralInterface(tokens: Tokens): TypeNode? {
    val keyword = tokens.keyword(DefaultToken.ROUND_BRACKET_OPEN) ?: return null;
    val fields = mutableListOf<TypeNode.StructuralInterfaceTypeNode.StructuralFieldNode>()
    val interfaceMessages = CompilerMessages()
    while (tokens.hasNext()) {
        val messages = CompilerMessages()
        tokens.skip(DefaultToken.WHITESPACE);
        val name = parseIdentifier(tokens);
        tokens.skip(DefaultToken.WHITESPACE);
        val type = parseExplicitType(tokens, messages)
        val element = TypeNode.StructuralInterfaceTypeNode.StructuralFieldNode(name, type)
        messages.apply(element);
        fields.add(element);
        tokens.skip(DefaultToken.WHITESPACE);
        val endDelimiter = tokens.peek().type
        when (endDelimiter) {
            DefaultToken.ROUND_BRACKET_CLOSED -> {
                tokens.next()
                break;
            }

            DefaultToken.COMMA -> {
                tokens.next()
            }

            DefaultToken.EOF -> {
                interfaceMessages.error(
                    "Scope was not closed, expected '}' to close the interface definition",
                    tokens.location,
                    CompilerMessage.Hint("Opened here", keyword)
                )
                break;
            }

            else -> {
                interfaceMessages.error(
                    "Expected a new filed delemited by ',' or  end of the interface using '}' after the field definition",
                    tokens.location
                )
                break;
            }
        }
    }
    return TypeNode.StructuralInterfaceTypeNode(fields, keyword.rangeTo(tokens.location));
}