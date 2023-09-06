package eu.nitok.jitsu.compiler.parser

import com.niton.parser.ast.SequenceNode
import com.niton.parser.grammar.api.Grammar
import com.niton.parser.grammar.api.Grammar.*
import com.niton.parser.token.DefaultToken
import eu.nitok.jitsu.compiler.ast.BiOperator
import eu.nitok.jitsu.compiler.ast.ExpressionType


internal var operatorExpression = build(ExpressionType.OPERATION_EXPRESSION)
    .grammar(anyOf(*nonRecursiveExpression)).add("left")
    .token(DefaultToken.WHITESPACE).ignore().add()
    .grammar(anyOf(*BiOperator.entries.map { keyword(it.rune).named(it) }.toTypedArray()).display("operation expression (+, -, % ...)")).add("operator")
    .token(DefaultToken.WHITESPACE).ignore().add()
    .grammar(ANY_EXPRESSION_NAME).add("right")
    .get();