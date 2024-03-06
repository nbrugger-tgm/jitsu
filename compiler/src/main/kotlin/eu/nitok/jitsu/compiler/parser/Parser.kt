package eu.nitok.jitsu.compiler.parser

import com.niton.jainparse.token.DefaultToken
import com.niton.jainparse.token.DefaultToken.*
import com.niton.jainparse.token.TokenSource
import com.niton.jainparse.token.TokenStream
import com.niton.jainparse.token.Tokenizer
import com.niton.jainparse.token.Tokenizer.AssignedToken
import eu.nitok.jitsu.compiler.ast.*
import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage
import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage.Hint
import eu.nitok.jitsu.compiler.model.BitSize
import eu.nitok.jitsu.compiler.parser.parsers.parseFunction
import eu.nitok.jitsu.compiler.parser.parsers.parseIdentifier
import eu.nitok.jitsu.compiler.parser.parsers.parseStructuralInterface
import java.io.Reader
import java.io.StringReader
import java.net.URI
import kotlin.jvm.optionals.getOrNull

typealias Tokens = TokenStream<DefaultToken>

fun parseFile(input: Reader, uri: URI): SourceFileNode {
    val tokenSource = TokenSource(input, tokenizer);
    val tokens = TokenStream.of(tokenSource)
    val statements = mutableListOf<StatementNode>()
    val sourceFileNode = SourceFileNode(uri.toString(), statements)
    parseStatements(tokens, statements, sourceFileNode::error)
    return sourceFileNode;
}

fun parseFile(txt: String, uri: URI): SourceFileNode {
    val tokens = tokenize(txt)
    var statements = mutableListOf<StatementNode>()
    val sourceFileNode = SourceFileNode(uri.toString(), statements)
    parseStatements(tokens, statements, sourceFileNode::error)
    return sourceFileNode;
}

fun parseStatements(
    tokens: Tokens,
    statements: MutableList<StatementNode>,
    containerNode: (CompilerMessage) -> Unit
) {
    while (tokens.hasNext()) {
        tokens.skipWhitespace();
        when (val x = parseStatement(tokens)) {
            is StatementNode -> statements.add(x);
            null -> {
                tokens.skipWhitespace()
                val lastToken = tokens.index()
                val invalid = tokens.skipUntil(ROUND_BRACKET_CLOSED, SEMICOLON, NEW_LINE)
                if (lastToken == tokens.index()) {
                    //nothing invalid was skipped - so end of block
                    break;
                }
                tokens.skip(SEMICOLON)
                containerNode(CompilerMessage("Expected a statement", invalid))
            };
        }
    }
}

private fun Tokens.skipUntil(vararg stoppers: DefaultToken): Range {
    return range {
        while (peekOptional().map { !stoppers.contains(it.type) }.orElse(false)) skip()
    }.location
}

fun Tokens.skip(vararg toSkip: DefaultToken) {
    while (this.hasNext() && toSkip.contains(this.peek().type)) {
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
    return parseFunction(tokens) ?: parseExecutableStatement(tokens) {
        parseVariableDeclaration(it) ?: parseAssignment(it) ?: parseReturnStatement(it)
    }
}

fun parseReturnStatement(tokens: Tokens): StatementNode? {
    val kw = tokens.keyword("return") ?: return null;
    tokens.skipWhitespace()
    val value = parseExpression(tokens);
    return StatementNode.ReturnNode(value, kw.rangeTo(value?.location ?: kw), kw)
}


private fun parseExecutableStatement(tokens: Tokens, statmentFn: (Tokens) -> StatementNode?): StatementNode? {
    val res = statmentFn(tokens) ?: return null;
    val endOfStatementPos = tokens.location.toRange();
    tokens.skipWhitespace()
    val semicolon = tokens.peekOptional();
    if (semicolon.map { it.type }.orElse(null) != SEMICOLON) {
        res.error(CompilerMessage("Expect semicolon at end of statement!", endOfStatementPos))
    } else {
        tokens.skip()
    }
    return res;
}

fun parseVariableDeclaration(tokens: Tokens): StatementNode.VariableDeclarationNode? {
    val kw = tokens.keyword("var") ?: return null;
    val messages = CompilerMessages()
    tokens.skipWhitespace();
    val name = parseIdentifier(tokens);
    tokens.skipWhitespace();
    val type = parseOptionalExplicitType(tokens, messages::error)
    tokens.skipWhitespace();
    val eq = tokens.keyword(EQUAL);
    if (eq == null) {
        messages.error("Variables need an initial value!", tokens.location.toRange())
    } else {
        tokens.skipWhitespace()
    }
    val expression = parseExpression(tokens);

    if (expression == null)
        messages.error("Expected value to assign to '${name?.value}'", tokens.location.toRange())
    return StatementNode.VariableDeclarationNode(
        name,
        type,
        expression,
        kw
    ).withMessages(messages);
}


fun Tokens.skipWhitespace() {
    skip(WHITESPACE, NEW_LINE)
}

fun Tokens.keyword(s: DefaultToken): Range? {
    elevate()
    if (!hasNext()) return null;
    val token = range { next() }
    return if (token.value.type == s) {
        commit()
        token.location
    } else {
        rollback()
        null
    }
}

fun parseType(tokens: Tokens): TypeNode? {
    val firstType = parseSingleType(tokens) ?: return null;
    val union = parseUnion(firstType, tokens);
    return union ?: firstType;
}

fun parseSingleType(tokens: Tokens): TypeNode? {
    tokens.keyword("int")?.let {
        return TypeNode.IntTypeNode(BitSize.BIT_32, it)
    }
    tokens.keyword("void")?.let {
        return TypeNode.VoidTypeNode(it)
    }
    tokens.keyword("float")?.let {
        return TypeNode.FloatTypeNode(BitSize.BIT_32, it)
    }

    val structuralInterface = parseStructuralInterface(tokens);
    if (structuralInterface != null) {
        return structuralInterface;
    }
    val typeReference = parseIdentifier(tokens) ?: return null;
    val namedType = TypeNode.NameTypeNode(typeReference, listOf(), typeReference.location);
    return namedType;
}

fun parseOptionalExplicitType(tokens: Tokens, messages: (CompilerMessage) -> Unit): TypeNode? {
    val colon = tokens.keyword(COLON) ?: return null;
    tokens.skipWhitespace()
    val type = parseType(tokens)
    if (type == null) {
        messages(
            CompilerMessage(
                "Expected type", tokens.location.toRange(), Hint("Colon starts type definition", colon)
            )
        );
    }
    return type;
}

fun parseExplicitType(
    tokens: Tokens,
    messages: CompilerMessages
): TypeNode? {
    if (tokens.keyword(COLON) == null) {
        messages.error("Expected a type definition starting with a ':'", tokens.location.toRange())
        tokens.location
    } else {
        tokens.skipWhitespace()
    }
    val type = parseType(tokens);
    return type
}

fun parseUnion(firstType: TypeNode, tokens: TokenStream<DefaultToken>): TypeNode.UnionTypeNode? {
    if (!tokens.hasNext()) return null;
    tokens.elevate()
    val (pipe, pipeLocation) = tokens.range { next() }
    if (pipe.type != PIPE) {
        tokens.rollback()
        return null;
    }
    val types = mutableListOf(firstType);
    val messages = CompilerMessages();
    while (tokens.hasNext()) {
        tokens.skipWhitespace();
        val type = parseSingleType(tokens);
        if (type == null) {
            messages.error(
                CompilerMessage(
                    "Expect type for union",
                    tokens.location.toRange(),
                    Hint("Union starts here", pipeLocation)
                )
            )
        } else {
            types.add(type);
        }
        tokens.skipWhitespace();
        if (tokens.peekOptional().getOrNull()?.type == PIPE) {
            tokens.next()
        } else {
            break;
        }
    }
    return TypeNode.UnionTypeNode(types);
}

fun parseExpression(tokens: Tokens): ExpressionNode? {
    var expressionNode = parseSingleExpression(tokens)
    while(expressionNode != null) {
        val composite = parseCompositExpression(tokens, expressionNode) ?: return expressionNode;
        expressionNode = composite;
    }
    return expressionNode;
}

private fun parseCompositExpression(
    tokens: Tokens,
    expressionNode: ExpressionNode
) = parseOperation(tokens, expressionNode)

private fun parseSingleExpression(tokens: Tokens) =
    parseIntLiteral(tokens) ?: parseVariableReference(tokens)

fun parseOperation(tokens: Tokens, left: ExpressionNode): ExpressionNode? {
    tokens.elevate()
    tokens.skipWhitespace()
    val op = tokens.expect(PLUS, MINUS, STAR, SLASH)
    if (op == null) {
        tokens.rollback();
        return null
    }
    tokens.commit();
    tokens.skipWhitespace()
    val right = parseSingleExpression(tokens);
    return ExpressionNode.OperationNode(
        left,
        Located(
            BiOperator.byRune(op.value.value)
                ?: throw IllegalStateException("No operator found for rune ${op.value.value}!"), op.location
        ),
        right
    )
}

fun parseVariableReference(tokens: Tokens): ExpressionNode? {
    return parseIdentifier(tokens)?.let { ExpressionNode.VariableReferenceNode(it) }
}

fun parseIntLiteral(tokens: Tokens): ExpressionNode? {
    val next = tokens.expect(NUMBER) ?: return null;
    return ExpressionNode.NumberLiteralNode.IntegerLiteralNode(next.value.value, next.location)
}

fun parseAssignment(tokens: Tokens): StatementNode.AssignmentNode? {
    tokens.elevate()
    val kw = parseIdentifier(tokens)
    if (kw == null) {
        tokens.rollback()
        return null
    }
    tokens.skipWhitespace();
    val eq = tokens.keyword(EQUAL);
    if (eq == null) {
        tokens.rollback()
        return null
    }
    tokens.skipWhitespace();
    val expression = parseExpression(tokens);
    return StatementNode.AssignmentNode(ExpressionNode.VariableReferenceNode(kw), expression).run {
        if (expression == null)
            this.error(CompilerMessage("Expected value to assign to '${kw.value}'", tokens.location.toRange()))
        this
    };
}

/**
 * @return The range of the keyword, or null if the keyword is not present
 */
fun Tokens.keyword(s: String): Range? {
    elevate()
    val (token, location) = range { nextOptional().getOrNull() }
    token ?: return null
    return if (token.value == s) {
        commit()
        location
    } else {
        rollback()
        null
    }
}

inline fun <T> Tokens.range(action: Tokens.() -> T): Located<T> {
    val start = location
    val res = action()
    val end = lastConsumedLocation
    return Located(res, start.rangeTo(end))
}

fun Tokens.expect(vararg token: DefaultToken): Located<AssignedToken<DefaultToken>>? {
    val next = peekOptional().getOrNull() ?: return null;
    if (token.contains(next.type)) {
        return range { next() }
    }
    return null;
}

val Tokens.lastConsumedLocation: Location
    get() {
        val loc = this.lastConsumedLocation();
        return Location(loc.fromLine, loc.fromColumn)
    }
val Tokens.location: Location
    get() = Location(this.line, this.column)
