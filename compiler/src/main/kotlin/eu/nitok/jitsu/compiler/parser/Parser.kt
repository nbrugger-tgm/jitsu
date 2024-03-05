package eu.nitok.jitsu.compiler.parser

import com.niton.jainparse.token.DefaultToken
import com.niton.jainparse.token.DefaultToken.*
import com.niton.jainparse.token.TokenSource
import com.niton.jainparse.token.TokenStream
import com.niton.jainparse.token.Tokenizer
import eu.nitok.jitsu.compiler.ast.*
import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage
import eu.nitok.jitsu.compiler.parser.parsers.parseFunction
import eu.nitok.jitsu.compiler.parser.parsers.parseIdentifier
import eu.nitok.jitsu.compiler.parser.parsers.parseStructuralInterface
import java.io.StringReader

typealias Tokens = TokenStream<DefaultToken>

fun parseFile(txt: String): SourceFileNode {
    val tokens = tokenize(txt)
    var statements = mutableListOf<StatementNode>()
    val sourceFileNode = SourceFileNode(statements)
    while (tokens.hasNext()) {
        tokens.skip(WHITESPACE);
        when (val x = parseStatement(tokens)) {
            is StatementNode -> statements.add(x);
            null -> {
                sourceFileNode.error(CompilerMessage("Expected a statement", tokens.location))
                tokens.skip()
            };
        }
    }
    return sourceFileNode;
}

fun Tokens.skip(vararg whitespace: DefaultToken) {
    while (this.hasNext() && whitespace.contains(this.peek().type)) {
        this.next()
    }
}
fun Tokens.skip(n: Int = 1) {
    var count = n;
    while (count-- > 0 && hasNext()) {
        next()
    }
}

private val tokenizer = Tokenizer(DefaultToken.entries)

private fun tokenize(txt: String): Tokens {
    val tokens = TokenSource(StringReader(txt), tokenizer);
    val tokenStream = TokenStream.of(tokens)
    return tokenStream
}

private fun parseStatement(tokens: Tokens): StatementNode? {
    return parseFunction(tokens)?: parseVariableDeclaration(tokens) ?: parseAssignment(tokens)
}

fun parseVariableDeclaration(tokens: Tokens): StatementNode.VariableDeclarationNode? {
    val kw = tokens.keyword("var") ?: return null;
    tokens.skip(WHITESPACE);
    val name = parseIdentifier(tokens);
    tokens.skip(WHITESPACE);
    val type = parseExplicitType(tokens)
    tokens.skip(WHITESPACE);
    val eq = tokens.keyword(EQUAL);
    val messages = CompilerMessages()
    if(eq == null) {
        messages.error("Variables need an initial value!", tokens.location)
    }
    val expression = parseExpression(tokens);
    return StatementNode.VariableDeclarationNode(name, type, expression, kw.rangeTo(expression.location), kw);
}

fun parseExplicitType(tokens: Tokens): TypeNode? {
    val colon = tokens.keyword(DefaultToken.COLON) ?: return null;
    val type = parseType(tokens);
    return type;
}

fun Tokens.keyword(s: DefaultToken): Range? {
    elevate()
    if(!hasNext()) return null;
    val token = range { next() }
    return if (token.value.type == s) {
        commit()
        token.location
    } else {
        rollback()
        null
    }
}

fun parseType(tokens: Tokens): TypeNode {
    val firstType = parseSingleType(tokens);
    val union = parseUnion(firstType, tokens);
    return union ?: firstType;
}

fun parseSingleType(tokens: Tokens): TypeNode {
    val structuralInterface = parseStructuralInterface(tokens);
    if (structuralInterface != null) {
        return structuralInterface;
    }
    val typeReference = parseIdentifier(tokens)
    val namedType = TypeNode.NameTypeNode(typeReference, listOf(), typeReference.location);
    return namedType;
}

fun parseExplicitType(
    tokens: Tokens,
    messages: CompilerMessages
): TypeNode {
    if (tokens.keyword(COLON) == null) {
        messages.error("Expected a type definition starting with a ':' after the field name", tokens.location)
        tokens.location
    } else {
        tokens.skip(WHITESPACE)
    }
    val type = parseType(tokens);
    return type
}

fun parseUnion(firstType: TypeNode, tokens: TokenStream<DefaultToken>): TypeNode.UnionTypeNode? {
    if(!tokens.hasNext()) return null;
    tokens.elevate()
    val pipe = tokens.next()
    if (pipe.type != PIPE) {
        tokens.rollback()
        return null;
    }
    val types = mutableListOf(firstType);
    while (true) {
        tokens.skip(WHITESPACE);
        val type = parseSingleType(tokens);
        types.add(type);
        tokens.skip(WHITESPACE);
        if (tokens.peek().type == PIPE) {
            tokens.next()
        } else {
            break;
        }
    }
    return TypeNode.UnionTypeNode(types, firstType.location.rangeTo(types.last().location));
}

fun parseExpression(tokens: TokenStream<DefaultToken>): ExpressionNode {
    return ExpressionNode.NumberLiteralNode.IntegerLiteralNode("0", tokens.location.toRange());
}

fun parseAssignment(tokens: Tokens): StatementNode.AssignmentNode? {
    tokens.elevate()
    val kw = parseIdentifier(tokens);
    tokens.skip(WHITESPACE);
    val eq = tokens.keyword(EQUAL);
    if(eq == null) {
        tokens.rollback()
        return null
    }
    val expression = parseExpression(tokens);
    return StatementNode.AssignmentNode(ExpressionNode.VariableLiteralNode(kw.value, kw.location), expression);
}

/**
 * @return The range of the keyword, or null if the keyword is not present
 */
fun Tokens.keyword(s: String): Range? {
    elevate()
    val token = range { next() }
    return if (token.value.value == s) {
        commit()
        token.location
    } else {
        rollback()
        null
    }
}

fun <T> Tokens.range(action: Tokens.() -> T): Located<T> {
    val start = location
    val res = action()
    val end = location
    return object : Located<T> {
        override val location = start.rangeTo(end);
        override val value = res
    };
}

val Tokens.location: Location
    get() = Location(this.line, this.column)
