package eu.nitok.jitsu.compiler.parser.recursivedescent

import com.niton.parser.token.DefaultToken
import com.niton.parser.token.DefaultToken.*
import com.niton.parser.token.Location
import com.niton.parser.token.TokenStream
import com.niton.parser.token.Tokenizer
import eu.nitok.jitsu.compiler.ast.*


private val Tokenizer.AssignedToken.type: DefaultToken
    get() {
        return com.niton.parser.token.DefaultToken.valueOf(name)
    }

typealias Tokens = TokenStream;

object Parser {
    fun file(tokens: Tokens): List<N<StatementNode>> {
        val statements = mutableListOf<N<StatementNode>>()
        skipWhitespace(tokens)
        while (tokens.hasNext()) {
            statements.add(statement(tokens))
            skipWhitespace(tokens)
        }
        return statements
    }

    private fun statement(tokens: Tokens): N<StatementNode> {
        return lineComment(tokens) ?: ifStatement(tokens) ?: switchStatement(tokens) ?: functionDeclaration(tokens)
        ?: variableDeclaration(
            tokens
        ) ?: {
            var expression = expression(tokens);
        } ?: {
            val next = tokens.next()
            N.Error(tokens.currentLocation(), "Not a statement")
        }
    }

    private fun lineComment(tokens: Tokens): N<StatementNode>? {
        TODO("Not yet implemented")
    }

    private fun expression(tokens: Tokens): N<ExpressionNode>? {
        fun atomic(tokens: Tokens): N<ExpressionNode>? {
            return ifStatement(tokens) ?: switchStatement(tokens) ?: functionDeclaration(tokens)
            ?: identifier(tokens)?.map {
                ExpressionNode.VariableLiteralNode(it.first, it.second)
            }
        }

        var start = atomic(tokens) ?: return null;
        while (true) {
            start = call(start, tokens) ?: fieldAccess(start, tokens) ?: biOperator(start, tokens) ?: monoOperator(
                start,
                tokens
            ) ?: return start;
        }

    }

    private fun monoOperator(start: N<ExpressionNode>, tokens: Tokens): N<ExpressionNode>? {
        return null;
    }

    private fun biOperator(start: N<ExpressionNode>, tokens: Tokens): N<ExpressionNode>? {
        tokens.elevate()
        skipWhitespace(tokens)
        val (operator, operatorLocation) = captureRange(tokens) { tokens.next() };
        val operatorType = BiOperator.entries.find { it.rune == operator.value };
        if (operatorType == null) {
            tokens.rollback()
            return null;
        }
        tokens.commit()
        skipWhitespace(tokens)
        var right = expression(tokens);
        if (right == null) {
            right = N.Error(tokens.currentLocation(), "Expected expression after ${operator.value} operator")
            tokens.skip(1) // skip the non-expression token
        }
        return N.Node(
            ExpressionNode.OperationNode(
                start,
                operatorType to operatorLocation,
                right,
                Location.range(start.location { it.location }, right.location { it.location })
            )
        )
    }

    private fun fieldAccess(start: N<ExpressionNode>, tokens: Tokens): N<ExpressionNode>? {
        var error: String? = null;
        val (field, fieldLoc) = captureRange(tokens) {
            tokens.elevate()
            skipWhitespace(tokens)
            if (tokens.peek().type != DOT) {
                tokens.rollback()
                return@captureRange null;
            } else {
                tokens.commit()
                tokens.next();
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
            if (start.type != LETTERS) {
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

    private fun <T> captureRange(tokens: Tokens, function: () -> T): Pair<T, Location> {
        val start = tokens.currentLocation();
        val returned = function();
        val end = tokens.currentLocation().minusChar(1);
        return returned to Location.range(start, end);
    }

    private fun ifStatement(tokens: Tokens): N<StatementNode.IfNode>? {
        TODO("Not yet implemented")
    }

    private fun call(method: N<ExpressionNode>, tokens: Tokens): N<StatementNode.FunctionCallNode>? {
        var error: String? = null;
        val nameLocation: Location;
        when (method) {
            is N.Node -> {
                when (method.value) {
                    is ExpressionNode.VariableLiteralNode -> nameLocation = method.value.location;
                    is ExpressionNode.FieldAccessNode -> nameLocation = method.value.fieldLocation;
                    else -> {
                        error =
                            "Only named expressions can be called. Capture the expression in a variable or field first";
                        nameLocation = method.location { it.location }
                    }
                }
            }

            is N.Error -> {
                nameLocation = method.node
            }
        }
        val (parameters, parameterLoc) = captureRange(tokens) { callParameterList(tokens) }
        if (parameters == null) {
            return null
        }
        val methodLocation = method.location { it.location }
        val range = methodLocation.rangeTo(parameterLoc)
        return if (error != null) {
            N.Error(range, error);
        } else {
            N.Node(StatementNode.FunctionCallNode(method, parameters, range, nameLocation))
        }
    }

    private fun callParameterList(tokens: Tokens): List<N<ExpressionNode>>? {
        skipWhitespace(tokens);
        if (tokens.peek().type != BRACKET_OPEN) {
            return null;
        } else {
            tokens.next();
        }
        skipWhitespace(tokens);
        val parameters = mutableListOf<N<ExpressionNode>>();
        while (true) {
            var expression = expression(tokens);
            if (expression == null) {
                expression = N.Error(tokens.currentLocation(), "Expected expression");
            }
            parameters.add(expression);
            skipWhitespace(tokens);
            val nextSymbol = tokens.next().type
            if (nextSymbol == COMMA) {
                tokens.next();
                skipWhitespace(tokens);
            } else if (nextSymbol == BRACKET_CLOSED) {
                break;
            } else if (nextSymbol == NEW_LINE || nextSymbol == EOF) {
                parameters.add(N.Error(tokens.currentLocation(), "Parameter list needs to be closed with ')'"));
                break;
            } else {
                parameters.add(N.Error(tokens.currentLocation(), "Expected ',' or ')'"));
            }
        }
        return parameters
    }

    private fun switchStatement(tokens: Tokens): N<StatementNode.SwitchNode>? {
        TODO("Not yet implemented")
    }

    private fun functionDeclaration(tokens: Tokens): N<StatementNode.FunctionDeclarationNode>? {
        TODO("Not yet implemented")
    }

    private fun variableDeclaration(tokens: Tokens): N<StatementNode.VariableDeclarationNode>? {
        TODO("Not yet implemented")
    }

    private fun skipWhitespace(tokens: Tokens) {
        while (checkNextToken(tokens, WHITESPACE, NEW_LINE)) {
            tokens.skip(1)
        }
    }

    private fun checkNextToken(tokens: Tokens, vararg token: DefaultToken): Boolean {
        val next = tokens.peek()
        return token.any { it.name == next.name }
    }
}

