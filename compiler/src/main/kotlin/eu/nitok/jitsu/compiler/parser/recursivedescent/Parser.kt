package eu.nitok.jitsu.compiler.parser.recursivedescent

import com.niton.parser.ast.ParsingResult
import com.niton.parser.token.DefaultToken
import com.niton.parser.token.DefaultToken.*
import com.niton.parser.token.ListTokenStream
import com.niton.parser.token.Location.range
import com.niton.parser.token.TokenStream
import com.niton.parser.token.Tokenizer
import com.niton.parser.token.Tokenizer.AssignedToken
import eu.nitok.jitsu.compiler.ast.*
import eu.nitok.jitsu.compiler.ast.AttributeNode.AttributeValueNode
import eu.nitok.jitsu.compiler.ast.ExpressionNode.OperationNode
import eu.nitok.jitsu.compiler.ast.ExpressionNode.StringLiteralNode.StringPart
import eu.nitok.jitsu.compiler.ast.StatementNode.AssignmentNode.AssignmentTarget
import eu.nitok.jitsu.compiler.ast.StatementNode.FunctionCallNode
import eu.nitok.jitsu.compiler.ast.StatementNode.FunctionDeclarationNode.ParameterNode
import eu.nitok.jitsu.compiler.ast.StatementNode.IfNode.ElseNode
import eu.nitok.jitsu.compiler.ast.StatementNode.IfNode.ElseNode.ElseBlockNode
import eu.nitok.jitsu.compiler.ast.StatementNode.MethodInvocationNode
import eu.nitok.jitsu.compiler.model.BitSize


private val Tokenizer.AssignedToken.type: DefaultToken?
    get() {
        if (name.equals("UNDEFINED")) return null;
        return com.niton.parser.token.DefaultToken.valueOf(name)
    }
private typealias ParseFn<T> = (tokens: Tokens) -> N<T>?
typealias Tokens = TokenStream;

private fun TokenStream.nextWithLocation(): Located<AssignedToken> {
    return captureRange(this) { next() }
}

private val tokenizer = Tokenizer();

public fun parseFile(txt: String): ParsingResult<List<N<StatementNode>>> {
    val tokens = tokenizer.tokenize(txt);
    if (!tokens.wasParsed()) {
        return ParsingResult.error(tokens.exception())
    }
    return ParsingResult.ok(Parser.file(ListTokenStream(tokens.unwrap())));
}

object Parser {
    fun file(tokens: Tokens): List<N<StatementNode>> {
        val statements = mutableListOf<N<StatementNode>>()
        skipWhitespace(tokens)
        while (true) {
            val statement = statement(tokens) ?: break
            statements.add(statement)
            skipWhitespace(tokens)
        }
        if (!checkNextToken(tokens, EOF)) {
            statements.add(N.Error(tokens.currentLocation(), "Expected end of file or statement"))
        }

        return statements
    }

    private fun statement(tokens: Tokens): N<StatementNode>? {
        return atomicStatement(tokens) ?: run {
            when (val expression = expression(tokens)) {
                null -> null
                is N.Node -> when (val stat = expression.value) {
                    is FunctionCallNode -> N.Node(stat).warning(semicolonWarning(tokens))
                    is MethodInvocationNode -> N.Node(stat).warning(semicolonWarning(tokens))
                    is StatementNode -> N.Node(stat)
                    else -> assignment(expression, tokens)
                }

                else -> assignment(expression, tokens)
            }
        }
    }

    private fun atomicStatement(tokens: Tokens): N<StatementNode>? {
        return lineComment(tokens) ?: variableDeclaration(tokens) ?: returnStatement(tokens) ?: yieldStatement(tokens) ?: typeDefinition(tokens) ?: interfaceDeclaration(tokens)
    }

    private fun typeDefinition(tokens: Tokens): N<StatementNode>? {
        tokens.elevate()
        val attributes = attributes(tokens)
        skipWhitespace(tokens)
        val (kw, kwLoc) = captureRange(tokens) { tokens.next() }
        if (kw.value != "type") {
            tokens.rollback()
            return null
        }
        tokens.commit()
        skipWhitespace(tokens)
        val name = identifier(tokens) ?: N.Error(tokens.currentLocation(), "Expected type name")
        skipWhitespace(tokens)
        var equals = tokens.peek();
        if (equals.type != EQUAL) {
            name.warning(N.Error(tokens.currentLocation(), "Expected '='"))
        } else {
            tokens.next()
        }
        skipWhitespace(tokens)
        val type = type(tokens) ?: N.Error(tokens.currentLocation(), "Expected type")
        skipWhitespace(tokens)
        return N.Node(
            StatementNode.TypeDefinitionNode(
                name,
                type,
                range(kwLoc, type.location { it.location }),
                kwLoc,
                attributes
            )
        )
    }

    private fun interfaceDeclaration(tokens: Tokens): N<TypeNode.InterfaceTypeNode>? {
        tokens.elevate()
        val attributes = attributes(tokens)
        skipWhitespace(tokens)
        val (kw, kwLoc )= captureRange(tokens){ tokens.next() }
        if(kw.value != "interface"){
            tokens.rollback()
            return null;
        }
        tokens.commit()
        skipWhitespace(tokens)
        val name = identifier(tokens) ?: N.Error(tokens.currentLocation(), "Expected interface name")
        skipWhitespace(tokens)
        val statements = signatures(tokens);

        return N.Node(TypeNode.InterfaceTypeNode(
            name,
            statements,
            range(kwLoc, statements.lastOrNull()?.location { it.location } ?: name.location),
            kwLoc,
            attributes
        ));
    }

    private fun returnStatement(tokens: Tokens): N<StatementNode>? {
        var keyword = tokens.peek();
        if (keyword.value != "return") {
            return null;
        }
        var (_, kwLoc) = captureRange(tokens) { tokens.next() }

        skipWhitespace(tokens)
        val expression = expression(tokens)
        skipWhitespace(tokens)
        val semicolonWarning = semicolonWarning(tokens)
        return N.Node(
            StatementNode.ReturnNode(
                expression,
                range(kwLoc, expression?.location { it.location } ?: kwLoc),
                kwLoc
            )
        )
            .warning(semicolonWarning)
    }

    private fun yieldStatement(tokens: Tokens): N<StatementNode>? {
        var keyword = tokens.peek();
        if (keyword.value != "yield") {
            return null;
        }
        var (_, kwLoc) = captureRange(tokens) { tokens.next() }

        skipWhitespace(tokens)
        val expression = expression(tokens)
        skipWhitespace(tokens)
        val semicolonWarning = semicolonWarning(tokens)
        return N.Node(
            StatementNode.YieldStatement(
                expression ?: N.Error(tokens.currentLocation(), "yield requires an expression"),
                range(kwLoc, expression?.location { it.location } ?: kwLoc),
                kwLoc
            )
        )
            .warning(semicolonWarning)
    }

    private fun assignment(expression: N<ExpressionNode>, tokens: Tokens): N<StatementNode>? {
        tokens.elevate()
        skipWhitespace(tokens)
        if (tokens.peek().type != EQUAL) {
            tokens.rollback()
            return null;
        } else {
            tokens.commit()
            tokens.next()
        }
        skipWhitespace(tokens)
        val valueExpression = expression(tokens) ?: N.Error(tokens.currentLocation(), "Expected expression")
        skipWhitespace(tokens)
        return when (expression) {
            is N.Error -> N.Node(variableAssignment(valueExpression, expression.casted(), tokens))
                .warning(semicolonWarning(tokens))

            is N.Node -> when (val exprNode = expression.value) {
                is StatementNode -> N.Node(exprNode).warning(semicolonWarning(tokens))
                is ExpressionNode.VariableLiteralNode -> {
                    N.Node(variableAssignment(valueExpression, N.Node(exprNode), tokens)).warning(semicolonWarning(tokens))
                }

                is ExpressionNode.FieldAccessNode -> {
                    N.Node(fieldAssignment(valueExpression, exprNode, tokens, expression)).warning(semicolonWarning(tokens))
                }

                else -> N.Error(expression.location { it.location }, "Not a statement")
            }
        }
    }

    private fun fieldAssignment(
        valueExpression: N<ExpressionNode>,
        exprNode: ExpressionNode.FieldAccessNode,
        tokens: Tokens,
        expression: N.Node<ExpressionNode>
    ): StatementNode.AssignmentNode {
        return StatementNode.AssignmentNode(
            N.Node(AssignmentTarget.FieldTarget(N.Node(exprNode))),
            valueExpression,
            range(
                expression.location { it.location },
                valueExpression.location { it.location } ?: tokens.currentLocation()
            ),
            exprNode.location
        )
    }

    private fun variableAssignment(
        value: N<ExpressionNode>?,
        exprNode: N<ExpressionNode.VariableLiteralNode>,
        tokens: Tokens
    ): StatementNode.AssignmentNode {
        val (name, location) = when (exprNode) {
            is N.Node -> exprNode.value.name to exprNode.value.location
            is N.Error -> "\$unknown" to exprNode.node
        }
        return StatementNode.AssignmentNode(
            N.Node(AssignmentTarget.VariableTarget(name, location)),
            value ?: N.Error(tokens.currentLocation(), "Expected expression"),
            range(
                exprNode.location { it.location },
                value?.location { it.location } ?: tokens.currentLocation()
            ),
            range(location, value?.location { it.location })
        )
    }

    private fun lineComment(tokens: Tokens): N<StatementNode>? {
        if (!checkNextToken(tokens, SLASH)) {
            return null;
        }
        tokens.elevate()
        tokens.next()
        if (!checkNextToken(tokens, SLASH)) {
            tokens.rollback()
            return null;
        }
        tokens.commit()
        tokens.next()
        val content = captureRange(tokens) {
            var content = ""
            while (tokens.hasNext()) {
                val next = tokens.peek()
                if (next.type == NEW_LINE) {
                    break;
                }
                content += next.value;
                tokens.next()
            }
            return@captureRange content
        }
        return N.Node(StatementNode.LineCommentNode(content, range(content.second.minusChar(2), content.second)))
    }

    private fun literalExpression(tokens: Tokens) =
        numberLiteral(tokens) ?: variableLiteral(tokens) ?: stringLiteral(tokens) ?: booleanLiteral(tokens)

    private fun booleanLiteral(tokens: Tokens): N<ExpressionNode.BooleanLiteralNode>? {
        val tkn = tokens.peek();
        return tkn.value.toBooleanStrictOrNull()?.let {
            N.Node(ExpressionNode.BooleanLiteralNode(it, tokens.nextWithLocation().second))
        }
    }

    private fun stringLiteral(tokens: Tokens): N<ExpressionNode.StringLiteralNode>? {
        if (tokens.peek().type != DOUBLEQUOTE) return null;
        val (str, textLoc) = captureRange(tokens) {
            tokens.next()
            stringContent(tokens)
        }
        return N.Node(ExpressionNode.StringLiteralNode(str, textLoc))
    }

    private fun stringContent(tokens: Tokens): MutableList<N<StringPart>> {
        val parts = mutableListOf<N<StringPart>>()
        while (true) {
            val (token, tokenLoc) = tokens.nextWithLocation();
            val part = when (token.type) {
                BACK_SLASH -> N.Node(
                    StringPart.EscapeSequence(
                        "\\${tokens.next().value}",
                        range(tokenLoc, tokens.currentLocation())
                    )
                )

                DOLLAR -> {
                    val identifier = identifier(tokens)
                    val mappedIdentifier = identifier?.map { StringPart.Literal(it.first, it.second, tokenLoc) }
                    mappedIdentifier ?: expressionStringTemplate(tokens, tokenLoc) ?: N.Error(
                        tokens.currentLocation(),
                        "Invalid template expression. Templates are formatted like this: \$identifier \${ someExpression.do() }"
                    )
                }

                DOUBLEQUOTE -> break;
                NEW_LINE, EOF -> N.Error(
                    tokenLoc.fromChar(),
                    "expecting string to be closed with \" - string are not multiline"
                )

                else -> N.Node(StringPart.Charsequence(token.value, tokenLoc))
            }
            parts.add(part)
        }
        return parts
    }

    private fun expressionStringTemplate(
        tokens: Tokens,
        second: Location
    ): N<StringPart>? {
        val openBracket = tokens.peek();
        if (openBracket.type != ROUND_BRACKET_OPEN) {
            return null;
        }
        val (_, openBracketLoc) = tokens.nextWithLocation()
        val expr = expression(tokens) ?: N.Error(tokens.currentLocation(), "Expecting expression in template")
        val bracketClosed = tokens.peek()
        val (bracketClosedLoc, expressionClosed) = run {
            val closed = bracketClosed.type == ROUND_BRACKET_CLOSED
            if (closed) tokens.next()
            tokens.currentLocation().minusChar(1) to closed
        }
        val part = N.Node(StringPart.Expression(expr, range(second, openBracketLoc), bracketClosedLoc))
        if (!expressionClosed) part.warning(
            N.Error(
                bracketClosedLoc,
                "Expected template expression to be closed with '}'"
            )
        )
        return part
    }

    fun expression(tokens: Tokens): N<ExpressionNode>? {


        fun atomic(tokens: Tokens): N<ExpressionNode>? {
            return ifStatement(tokens) ?: switchStatement(tokens) ?: functionDeclaration(tokens) ?: literalExpression(
                tokens
            ) ?: enclosed("expression", tokens, "(",")"){ expression(tokens) } ?: codeBlock(tokens)
        }

        val start = atomic(tokens) ?: return null;
        return recursiveExpression(start, tokens)
    }

    private fun numberLiteral(tokens: Tokens): N<ExpressionNode.NumberLiteralNode>? {
        val start = tokens.currentLocation();
        val token = tokens.peek();
        if (token.type != DOT && token.type != NUMBER) {
            return null;
        }
        val whole = if (token.type == DOT) {
            0
        } else {
            tokens.next()
            token.value.toInt()
        }
        val fractionSplitToken = tokens.peek().type
        if (fractionSplitToken != DOT && token.type != DOT) {
            val end = tokens.currentLocation();
            return N.Node(ExpressionNode.NumberLiteralNode.IntegerLiteralNode(whole.toString(), range(start, end)));
        }
        if (fractionSplitToken == DOT) {
            val fractionNumber = tokens.peek();
            return if (fractionNumber.type == NUMBER) {
                tokens.next()
                val fraction = fractionNumber.value.toInt()
                N.Node(
                    ExpressionNode.NumberLiteralNode.FloatLiteralNode(
                        "${whole}.${fraction}".toDouble(),
                        range(start, tokens.currentLocation().minusChar(1))
                    )
                )
            } else {
                N.Node(
                    ExpressionNode.NumberLiteralNode.FloatLiteralNode(
                        "${whole}.0".toDouble(),
                        range(start, tokens.currentLocation().minusChar(1))
                    )
                )
            }
        } else {
            throw RuntimeException("this should never happen")
        }
    }

    private fun recursiveExpression(
        start: N<ExpressionNode>,
        tokens: Tokens
    ): N<ExpressionNode> {
        var start1 = start
        while (true) {
            start1 = call(start1, tokens) ?: fieldAccess(start1, tokens) ?: biOperator(start1, tokens) ?: monoOperator(
                start1,
                tokens
            ) ?: indexAccess(start, tokens) ?: return start1;
        }
    }

    private fun indexAccess(start: N<ExpressionNode>, tokens: Tokens): N<ExpressionNode>? {
        if (tokens.peek().type != SQUARE_BRACKET_OPEN) {
            return null;
        }
        tokens.next();
        skipWhitespace(tokens)
        val index = expression(tokens) ?: N.Error(tokens.currentLocation(), "Expected expression as index")
        skipWhitespace(tokens)
        val (bracketClosed, bracketClosedLoc) = tokens.nextWithLocation();
        if (bracketClosed.type != SQUARE_BRACKET_CLOSED) {
            index.warning(N.Error(bracketClosedLoc, "Expected closing bracket"))
        }
        return N.Node(
            ExpressionNode.IndexAccessNode(
                start,
                index,
                range(start.location { it.location }, bracketClosedLoc)
            )
        )
    }

    private fun monoOperator(start: N<ExpressionNode>, tokens: Tokens): N<OperationNode>? {
        return null
    }

    private fun biOperator(start: N<ExpressionNode>, tokens: Tokens): N<ExpressionNode>? {
        tokens.elevate()
        skipWhitespace(tokens)
        val (operator, operatorLocation) = captureRange(tokens) {
            anyExcept(
                tokens,
                WHITESPACE,
                LETTERS,
                NUMBER,
                BRACKET_CLOSED
            ).joinToString("") { it.value }
        };
        val operatorType = BiOperator.entries.find { it.rune == operator };
        if (operatorType == null) {
            tokens.rollback()
            return null;
        }
        tokens.commit()
        skipWhitespace(tokens)
        var right = expression(tokens);
        if (right == null) {
            right = N.Error(tokens.currentLocation(), "Expected expression after ${operatorType.rune} operator")
            tokens.next() // skip the non-expression token
        }
        return N.Node(
            OperationNode(
                start,
                operatorType to operatorLocation,
                right,
                range(start.location { it.location }, right.location { it.location })
            )
        )
    }

    private fun anyExcept(tokens: Tokens, vararg tokenType: DefaultToken): List<AssignedToken> {
        val value: MutableList<AssignedToken> = mutableListOf();
        while (tokens.hasNext()) {
            val next = tokens.peek()
            if (tokenType.contains(next.type)) {
                break;
            }
            value += next;
            tokens.next()
        }
        return value;
    }

    private fun fieldAccess(start: N<ExpressionNode>, tokens: Tokens): N<ExpressionNode.FieldAccessNode>? {
        var error: String? = null;
        val (field, fieldLoc) = captureRange(tokens) {
            tokens.elevate()
            skipWhitespace(tokens)
            if (tokens.next().type != DOT) {
                tokens.rollback()
                return@captureRange null;
            } else {
                tokens.commit()
            }
            val next = tokens.peek().type
            if (next == WHITESPACE || next == NEW_LINE) {
                error = "No whitespace allowed between '.' and field name. You can put whitespace before the dot!";
            }
            var identifier = identifier(tokens);
            if (identifier == null) {
                identifier = N.Error(tokens.currentLocation(), "Expected field name");
                tokens.next();//skip the non-identifier token
            }
            return@captureRange identifier;
        }
        if (field == null) {
            return null;
        }
        val range = start.location { it.location }.rangeTo(fieldLoc)
        return when (val err = error) {
            null -> N.Node(ExpressionNode.FieldAccessNode(start, field, range))
            else -> N.Error(range, err)
        }
    }

    private fun identifier(tokens: Tokens): N<Located<String>>? {
        var wrongStartChar = false;
        val (content, range) = captureRange(tokens) {
            var content = ""
            val start = tokens.peek()
            wrongStartChar = start.type == NUMBER || start.type == DOLLAR || start.type == UNDERSCORE;
            if (start.type != LETTERS && !wrongStartChar || start.value == "fn") {
                return@captureRange null;
            } else {
                tokens.next();
            }
            content += start.value;
            while (tokens.hasNext()) {
                val next = tokens.peek();
                if (next.type == LETTERS || next.type == NUMBER || next.type == UNDERSCORE) {
                    content += next.value;
                    tokens.next();
                } else {
                    break;
                }
            }
            return@captureRange content
        }
        if (content == null)
            return null;
        return if (wrongStartChar) N.Error(range, "Identifiers can only start with a letter");
        else N.Node(content to range);
    }


    private fun ifStatement(tokens: Tokens): N<StatementNode.IfNode>? {
        tokens.elevate()
        val (ifKw, kwLoc) = tokens.nextWithLocation();
        var err: String? = null;
        var errLoc: Location? = null;
        if (ifKw.value != "if") {
            tokens.rollback()
            return null;
        }
        tokens.commit()
        skipWhitespace(tokens)
        val bracketToken = tokens.peek()
        if (bracketToken.type != BRACKET_OPEN) {
            err = "Expected condition to be in brackets"
            errLoc = tokens.currentLocation()
        } else {
            tokens.next();
        }
        skipWhitespace(tokens)
        val condition = expression(tokens) ?: run<Parser, N.Error<ExpressionNode>> {
            N.Error(tokens.currentLocation(), "Expected condition")
        }.let {
            if (err != null && errLoc != null) {
                it.warning(N.Error(errLoc, err))
            }
            it
        }
        skipWhitespace(tokens)
        val bracketCloseToken = tokens.peek()
        if (bracketCloseToken.type != BRACKET_CLOSED) {
            condition.warning(N.Error(tokens.currentLocation(), "Expected closing bracket after condition"))
        } else {
            tokens.next();
        }
        skipWhitespace(tokens)
        val body = codeBlock(tokens) ?: N.Error(tokens.currentLocation(), "Expected code block")
        skipWhitespace(tokens)
        val elseBranch = elseBranch(tokens);
        return N.Node(
            StatementNode.IfNode(
                condition,
                body,
                elseBranch,
                range(kwLoc, (elseBranch?.location { el -> el.location } ?: body.location { el -> el.location })),
                kwLoc
            )
        )
    }

    private fun elseBranch(tokens: Tokens): N<ElseNode>? {
        val start = tokens.currentLocation();
        val keyword = tokens.peek();
        if (keyword.value != "else") {
            return null;
        }
        tokens.next()
        val keywordEnd = tokens.currentLocation().minusChar(1);
        skipWhitespace(tokens)
        return if (tokens.peek().value == "if") {
            ifStatement(tokens)?.let {
                N.Node(ElseNode.ElseIfNode(it, range(start, keywordEnd)))
            } ?: N.Error(tokens.currentLocation(), "Expected id statement after 'else if'");

        } else {
            val elseBlock = codeBlock(tokens);
            if (elseBlock == null) {
                N.Error<ElseBlockNode>(range(start, keywordEnd), "Expected 'if' or code block after 'else'");
            } else {
                N.Node(ElseBlockNode(elseBlock, range(start, keywordEnd)));
            }
        }
    }

    private fun codeBlock(tokens: Tokens): N<StatementNode.CodeBlockNode>? {
        if (!checkNextToken(tokens, ROUND_BRACKET_OPEN)) {
            return null;
        }
        val start = tokens.currentLocation();
        tokens.next()
        skipWhitespace(tokens)
        tokens.elevate()
        val singleExpression = expression(tokens);
        if (singleExpression != null) {
            skipWhitespace(tokens)
            if (tokens.peek().type == ROUND_BRACKET_CLOSED) {
                tokens.commit()
                tokens.next()
                return N.Node(
                    StatementNode.CodeBlockNode.SingleExpressionCodeBlock(
                        singleExpression,
                        range(start, tokens.currentLocation().minusChar(1))
                    )
                )
            } else {
                tokens.rollback()
            }
        } else {
            tokens.rollback()
        }

        val statements = mutableListOf<N<StatementNode>>()
        while (true) {
            skipWhitespace(tokens)
            if (checkNextToken(tokens, ROUND_BRACKET_CLOSED)) {
                tokens.next()
                break;
            }
            val statement = statement(tokens)
            if (statement == null) {
                tokens.elevate()
                val (txt, unknownnLoc) = captureRange(tokens) { anyExcept(tokens, ROUND_BRACKET_CLOSED, SEMICOLON) };
                if (txt.isNotEmpty() && txt.last().type == EOF) {
                    tokens.rollback()
                    statements.add(
                        N.Error(
                            tokens.currentLocation(),
                            "Code block (starting at $start) needs to be closed with '}'"
                        )
                    )
                    break
                }
                tokens.commit()
                if (txt.isNotEmpty()) {
                    statements.add(N.Error(unknownnLoc, "Expected statement"))
                    continue
                }
                if (tokens.peek().type == SEMICOLON) {
                    tokens.next()
                    continue
                }
                skipWhitespace(tokens)
                val (expectedClosedBracket, bracketLocation) = tokens.nextWithLocation();
                if (expectedClosedBracket.type != ROUND_BRACKET_CLOSED) {
                    return N.Error(
                        range(start, bracketLocation),
                        "Code block needs to be closed with '}''",
                        bracketLocation
                    );
                }
                break;
            }
            statements.add(statement)
        }
        return N.Node(
            StatementNode.CodeBlockNode.StatementsCodeBlock(
                statements,
                range(start, tokens.currentLocation().minusChar(1))
            )
        )
    }

    private fun call(target: N<ExpressionNode>, tokens: Tokens): N<ExpressionNode>? {
        val (parameters, parameterLoc) = captureRange(tokens) { callParameterList(tokens) }
        if (parameters == null) {
            return null
        }
        if (target is N.Node && target.value is ExpressionNode.FieldAccessNode) {
            return N.Node(
                MethodInvocationNode(
                    N.Node(target.value),
                    parameters,
                    range(target.location { it.location }, parameterLoc)
                )
            )
        }
        val targetNode = when (target) {
            is N.Node -> {
                when (val actualTarget = target.value) {
                    is ExpressionNode.VariableLiteralNode -> N.Node(actualTarget)
                    else -> N.Error(
                        target.location { it.location },
                        "Only named expressions can be called. Capture the expression in a variable or field first"
                    )
                }
            }

            is N.Error -> target.casted()
        }
        return N.Node(
            FunctionCallNode(
                targetNode,
                parameters,
                range(target.location { it.location }, parameterLoc)
            )
        )
    }

    private fun semicolonWarning(tokens: Tokens): N.Error<Any>? =
        if (tokens.peek().type != SEMICOLON) {
            N.Error(tokens.currentLocation(), "Expected ';' after variable declaration")
        } else {
            tokens.next(); null
        }

    private fun callParameterList(tokens: Tokens): List<N<ExpressionNode>>? {
        return enclosedList("call parameters", tokens, "(", ",", ")", true) { expression(tokens) }
    }

    private fun switchStatement(tokens: Tokens): N<StatementNode.SwitchNode>? {
        val keyword = tokens.peek();
        if (keyword.value != "switch") return null;
        var keywordLoc = captureRange(tokens) { tokens.next() }
        skipWhitespace(tokens)
        val bracketOpen = tokens.peek();
        if (bracketOpen.type != BRACKET_OPEN) {
            return N.Error(tokens.currentLocation(), "Expected '(' after switch")
        } else {
            tokens.next()
        }
        skipWhitespace(tokens)
        val expression = expression(tokens) ?: N.Error(tokens.currentLocation(), "Expected expression")
        skipWhitespace(tokens)
        val bracketClose = tokens.peek();
        if (bracketClose.type != BRACKET_CLOSED) {
            return N.Error(tokens.currentLocation(), "Expected ')' after switch expression")
        } else {
            tokens.next()
        }
        skipWhitespace(tokens)
        val body = codeBlock(tokens) ?: N.Error(tokens.currentLocation(), "Expected code block")
        skipWhitespace(tokens)
        return N.Node(
            StatementNode.SwitchNode(
                expression,
                listOf(),
                range(keywordLoc.second, body.location { it.location }),
                keywordLoc.second
            )
        )
    }

    private fun functionDeclarationParameter(tokens: Tokens): N<ParameterNode>? {
        val name: N<Located<String>> = identifier(tokens) ?: return null;
        skipWhitespace(tokens)

        val typeDef = if (tokens.peek().type != COLON) {
            N.Error(tokens.currentLocation(), "Function parameters require a type, separated by ':'")
        } else {
            tokens.next()
            skipWhitespace(tokens)
            type(tokens) ?: N.Error(tokens.currentLocation(), "Expected type")
        }

        skipWhitespace(tokens)
        val defaultValue = if (tokens.peek().type == EQUAL) {
            tokens.next()
            skipWhitespace(tokens)
            valueLiteral(tokens) ?: variableLiteral(tokens) ?: N.Error(
                tokens.currentLocation(),
                "Expected constant for default value"
            )
        } else {
            null
        }


        return N.Node(
            ParameterNode(
                name,
                typeDef,
                defaultValue,
                range(
                    name.location { it.second },
                    defaultValue?.location { it.location } ?: typeDef.location { it.location }
                )
            )
        );
    }

    private fun valueLiteral(tokens: Tokens): N<ExpressionNode.VariableLiteralNode>? {
        var x = tokens.next()
        return N.Error(tokens.currentLocation(), "Literal no found lol")
    }

    private fun variableLiteral(tokens: Tokens) = identifier(tokens)?.map {
        ExpressionNode.VariableLiteralNode(it.first, it.second)
    }

    private fun functionDeclaration(tokens: Tokens): N<StatementNode.FunctionDeclarationNode>? {
        tokens.elevate();
        var (attrs, attrLoc) = captureRange(tokens) { attributes(tokens) }
        skipWhitespace(tokens)
        val (funKw, kwLoc) = captureRange(tokens) { tokens.next() }
        if (funKw.value != "fn") {
            tokens.rollback()
            return null;
        }
        tokens.commit()
        val warnings = mutableListOf<N.Error<Any>>()

        skipWhitespace(tokens)
        val name = identifier(tokens) ?: N.Error(tokens.currentLocation(), "Expected function name")
        skipWhitespace(tokens)
        val parameters = functionParameterDeclarations(tokens)
        skipWhitespace(tokens)
        val returnType = if (tokens.peek().type == COLON) {
            tokens.next()
            skipWhitespace(tokens)
            type(tokens) ?: N.Error(tokens.currentLocation(), "Expected return type after ':'")
        } else {
            null
        }
        skipWhitespace(tokens)
        val body = codeBlock(tokens) ?: N.Error(tokens.currentLocation(), "Expected code block/function body")
        skipWhitespace(tokens)

        val node = N.Node(
            StatementNode.FunctionDeclarationNode(
                name,
                parameters,
                returnType,
                body,
                range(attrLoc, body.location { it.location }),
                kwLoc,
                attrs
            )
        );
        node.warnings.addAll(warnings);
        return node;
    }

    private fun functionParameterDeclarations(
        tokens: Tokens
    ): List<N<ParameterNode>> {
        return enclosedList("parameters", tokens, "(", ",", ")", false) {
            functionDeclarationParameter(tokens)
        }?: emptyList()
    }

    private fun variableDeclaration(tokens: Tokens): N<StatementNode.VariableDeclarationNode>? {
        val varKw = tokens.peek();
        if (varKw.value != "var" && varKw.value != "val") {
            return null;
        }
        val (_, kwLoc) = tokens.nextWithLocation()
        skipWhitespace(tokens)
        val name = identifier(tokens) ?: N.Error(tokens.currentLocation(), "Expected variable name")
        skipWhitespace(tokens)
        val type = if (tokens.peek().type == COLON) {
            tokens.next()
            skipWhitespace(tokens)
            type(tokens)
        } else {
            null
        };
        skipWhitespace(tokens)
        val value = if (tokens.peek().type == EQUAL) {
            tokens.next()
            skipWhitespace(tokens)
            expression(tokens) ?: N.Error(tokens.currentLocation(), "Expected expression")
        } else {
            null
        }
        skipWhitespace(tokens)
        return N.Node(
            StatementNode.VariableDeclarationNode(
                name,
                type,
                value,
                range(
                    kwLoc,
                    (value?.location { it.location } ?: type?.location { it.location } ?: name.location { it.second })
                ),
                kwLoc
            )
        ).warning(semicolonWarning(tokens))
    }

    private fun type(tokens: Tokens): N<TypeNode>? {
        var type = atomicType(tokens) ?: return null
        while (true) {
            type = arrayType(tokens, type) ?: unionType(tokens, type) ?: break
        }
        return type
    }

    private fun unionType(tokens: Tokens, type: N<TypeNode>): N<TypeNode>? {
        val options = mutableListOf(type)
        while (true) {
            tokens.elevate()
            skipWhitespace(tokens)
            if (tokens.peek().type != PIPE) {
                tokens.rollback()
                if (options.size == 1) return null;
                return N.Node(
                    TypeNode.UnionTypeNode(
                        options,
                        range(type.location { it.location }, tokens.currentLocation().minusChar(1))
                    )
                );
            } else {
                tokens.commit()
                tokens.next()
            }
            skipWhitespace(tokens)
            val nextType = nonUnionType(tokens) ?: N.Error(tokens.currentLocation(), "Expected type")
            options.add(nextType)
        }
    }

    private fun attributes(tokens: Tokens): List<N<AttributeNode>> {
        val attributes = mutableListOf<N<AttributeNode>>()
        while (true) {
            val attribute = attribute(tokens) ?: break;
            attributes.add(attribute)
        }
        return attributes
    }

    private fun attribute(tokens: Tokens): N<AttributeNode>? {
        val at = tokens.peek()
        if (at.value != "@") {
            return null;
        }
        val atLoc = captureRange(tokens) { tokens.next() }.second
        val name = identifier(tokens) ?: N.Error(tokens.currentLocation(), "Expected attribute name")
        val bracketOpen = tokens.peek();
        if (bracketOpen.type == WHITESPACE || bracketOpen.type == NEW_LINE) {
            return N.Node(AttributeNode(name, listOf(), range(atLoc, name.location { it.second })))
        }
        val parameters = enclosedList("attribute parameters", tokens, "(", ",", ")", false) {
            val valueName = identifier(tokens) ?: return@enclosedList null;
            val separator = tokens.peek()
            if (separator.type != COLON) {
                valueName.warning(N.Error(tokens.currentLocation(), "Expected ':' after parameter name"))
            } else {
                tokens.next()
            }
            skipWhitespace(tokens)
            val parameter = literalExpression(tokens) ?: N.Error(
                tokens.currentLocation(),
                "Expected value for parameter ${valueName.map { it.first }}"
            )
            N.Node(AttributeValueNode(valueName, parameter))
        }?: emptyList()

        skipWhitespace(tokens)

        return N.Node(
            AttributeNode(
                name,
                parameters,
                range(atLoc, tokens.currentLocation().minusChar(1)),
            )
        )
    }

    private fun nonUnionType(tokens: Tokens): N<TypeNode>? {
        var type = atomicType(tokens) ?: return null
        while (true) {
            type = arrayType(tokens, type) ?: break
        }
        return type
    }

    private fun arrayType(
        tokens: Tokens,
        type: N<TypeNode>
    ): N<TypeNode.ArrayTypeNode>? {
        val opening = tokens.peek()
        if (opening.type != SQUARE_BRACKET_OPEN) {
            return null;
        }
        tokens.next();
        skipWhitespace(tokens)
        val constant = numberLiteral(tokens) ?: variableLiteral(tokens);
        skipWhitespace(tokens)
        val (closing, closingLocation) = tokens.nextWithLocation()
        val node = N.Node(TypeNode.ArrayTypeNode(type, constant, range(type.location { it.location }, closingLocation)))
        if (closing.type != SQUARE_BRACKET_CLOSED) {
            node.warning(N.Error(tokens.currentLocation(), "Expected closing the array ']'"))
        }
        return node
    }

    private fun atomicType(tokens: Tokens): N<TypeNode>? {
        val enclosed = enclosedType(tokens)
        if (enclosed != null)
            return enclosed;
        var (keyword, kwLoc) = captureRange(tokens) { literalExpression(tokens) }
        if (keyword == null) return null;
        if (keyword is N.Error) {
            return N.Error(kwLoc, "Types need to start with a literal or typename")
        }
        keyword = keyword as N.Node
        return when (val expr = keyword.value) {
            is ExpressionNode.NumberLiteralNode.FloatLiteralNode,
            is ExpressionNode.NumberLiteralNode.IntegerLiteralNode,
            is ExpressionNode.StringLiteralNode,
            is ExpressionNode.BooleanLiteralNode -> N.Node(TypeNode.ValueTypeNode(keyword, kwLoc))

            is ExpressionNode.VariableLiteralNode -> {
                N.Node(
                    when (expr.name) {
                        "enum" -> {
                            val constants = enumConstants(tokens)
                            TypeNode.EnumDeclarationNode(
                                constants,
                                range(kwLoc, tokens.currentLocation().minusChar(1)),
                                kwLoc
                            )
                        }

                        "void" -> TypeNode.VoidTypeNode(expr.location)
                        "interface" -> {
                            skipWhitespace(tokens)
                            val name = identifier(tokens);
                            name?.warning(N.Error(tokens.currentLocation(), "inline types cannot be named"))
                            skipWhitespace(tokens)
                            val functions = signatures(tokens)
                            TypeNode.InterfaceTypeNode(
                                name,
                                functions,
                                range(kwLoc, tokens.currentLocation().minusChar(1)),
                                kwLoc,
                                listOf()
                            )
                        }

                        "i64", "int" -> TypeNode.IntTypeNode(N.Node(BitSize.BIT_64), kwLoc)
                        "i32" -> TypeNode.IntTypeNode(N.Node(BitSize.BIT_32), kwLoc)
                        "i16" -> TypeNode.IntTypeNode(N.Node(BitSize.BIT_16), kwLoc)
                        "i8", "byte" -> TypeNode.IntTypeNode(N.Node(BitSize.BIT_8), kwLoc)
                        "String" -> TypeNode.StringTypeNode(kwLoc)
                        else -> TypeNode.NamedTypeNode(N.Node(expr.name to expr.location), listOf(), kwLoc)
                    }
                )
            }

            else -> throw RuntimeException("should never happen")
        }
    }

    private fun signatures(tokens: Tokens): List<N<TypeNode.InterfaceTypeNode.FunctionSignatureNode>> {
        return enclosedList("interface body", tokens, "{", ",", "}", false) { functionSignature(tokens) }?: emptyList()
    }

    private fun <T> enclosedList(
        name: String,
        tokens: Tokens,
        opening: String,
        delimiter: String?,
        closing: String,
        nullable: Boolean,
        elementParser: ParseFn<T>
    ): List<N<T>>? {
        val elements = mutableListOf<N<T>>()
        val start = tokens.currentLocation();
        if (tokens.peek().value != opening) {
            if(nullable)
                return null;
            elements.add(N.Error(start, "Expect $name to start with '$opening'"))
        } else {
            tokens.next()
        }
        while (true) {
            skipWhitespace(tokens)
            val element = elementParser(tokens);
            elements.add(element ?: break)
            skipWhitespace(tokens)
            val next = tokens.peek();

            if (next.value == closing) {
                tokens.next();
                return elements;
            }
            if (delimiter != null) {
                if (next.value != delimiter) {
                    element.warning(N.Error(tokens.currentLocation(), "Expect '${delimiter}' or '${closing}'"))
                } else {
                    tokens.next()
                }
            }
        }
        val next = tokens.peek();

        if (next.value == closing) {
            tokens.next();
        } else {
            elements.add(N.Error(tokens.currentLocation(), "Expect $name to be closed with '${closing}'"))
        }
        return elements;
    }

    private fun functionSignature(tokens: Tokens): N<TypeNode.InterfaceTypeNode.FunctionSignatureNode>? {
        val name = identifier(tokens) ?: return null;
        skipWhitespace(tokens)
        val warnings = mutableListOf<N.Error<Any>>()
        val functionTypeSignatureNode = functionTypeSignatureNode(tokens)
        return N.Node(
            TypeNode.InterfaceTypeNode.FunctionSignatureNode(
                name,
                functionTypeSignatureNode,
                range(name.location, functionTypeSignatureNode.location)
            )
        ).warning(warnings.firstOrNull())
    }

    private fun functionTypeSignatureNode(
        tokens: Tokens,
    ): TypeNode.FunctionTypeSignatureNode {
        val (parameters, paramsLoc) = captureRange(tokens) {
            functionParameterDeclarations(tokens)
        }
        skipWhitespace(tokens)
        val returnType = if (tokens.peek().type == COLON) {
            tokens.next()
            skipWhitespace(tokens)
            type(tokens) ?: N.Error(tokens.currentLocation(), "Expected return type after ':'")
        } else {
            null
        }
        return TypeNode.FunctionTypeSignatureNode(
            returnType,
            parameters,
            range(paramsLoc, returnType?.location { it.location } ?: paramsLoc)
        )
    }

    private fun enclosedType(tokens: Tokens): N<TypeNode>? {
        val bracketOpen = tokens.peek()
        if (bracketOpen.type != BRACKET_OPEN) {
            return null;
        }
        tokens.next()
        skipWhitespace(tokens)
        val type = type(tokens) ?: N.Error(tokens.currentLocation(), "Expected type")
        skipWhitespace(tokens)
        val bracketClosed = tokens.peek()
        if (bracketClosed.type != BRACKET_CLOSED) {
            type.warning(N.Error(tokens.currentLocation(), "Expected closing bracket"))
        } else {
            tokens.next()
        }
        return type
    }

    private fun enumConstants(tokens: Tokens): List<N<TypeNode.EnumDeclarationNode.ConstantNode>> {
        val constants = mutableListOf<N<TypeNode.EnumDeclarationNode.ConstantNode>>()
        if (tokens.peek().type != BRACKET_OPEN) {
            constants.add(N.Error(tokens.currentLocation(), "Expect '('"))
        }
        tokens.next()
        while (true) {
            skipWhitespace(tokens)
            val constant = identifier(tokens);
            constants.add(constant?.map { TypeNode.EnumDeclarationNode.ConstantNode(it.first, it.second) } ?: break)
            skipWhitespace(tokens)
            val next = tokens.peek();

            if (next.type == BRACKET_CLOSED) {
                tokens.next();
                return constants;
            }
            if (next.type != COMMA) {
                constants.add(
                    N.Error(
                        tokens.currentLocation(),
                        "Enum options should be separated using a comma (',') or closed with ')'"
                    )
                )
            } else {
                tokens.next()
            }
        }
        val next = tokens.peek();

        if (next.type == BRACKET_CLOSED) {
            tokens.next();
        }
        return constants;
    }


}

fun<T> enclosed(name: String, tokens: Tokens,opener: String, closer: String, parser: ParseFn<T>): N<T>? {
    if (tokens.peek().value != opener) {
        return null;
    }
    val (_, openBracketLoc) = tokens.nextWithLocation()
    val expr = parser(tokens) ?: N.Error(tokens.currentLocation(), "Expecting $name")
    val bracketClosed = tokens.peek()
    val (bracketClosedLoc, expressionClosed) = run {
        val closed = bracketClosed.type == BRACKET_CLOSED
        if (closed) tokens.next()
        tokens.currentLocation().minusChar(1) to closed
    }
    if (!expressionClosed) expr.warning(
        N.Error(
            bracketClosedLoc,
            "Expected $name to be closed with '$closer'"
        )
    )
    return expr
}

private fun <T> captureRange(tokens: Tokens, function: () -> T): Pair<T, Location> {
    val start = tokens.currentLocation();
    val returned = function();
    val end = tokens.currentLocation().minusChar(1);
    return returned to range(start, end);
}

private fun skipWhitespace(tokens: Tokens) {
    while (tokens.hasNext() && checkNextToken(tokens, WHITESPACE, NEW_LINE)) {
        tokens.next()
    }
}

private fun checkNextToken(tokens: Tokens, vararg token: DefaultToken): Boolean {
    val next = tokens.peek()
    return token.any { it.name == next.name }
}