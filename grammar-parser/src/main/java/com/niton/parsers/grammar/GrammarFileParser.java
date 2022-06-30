package com.niton.parsers.grammar;

import com.niton.parser.Parser;
import com.niton.parser.ast.AstNode;
import com.niton.parser.ast.ReducedNode;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.types.ChainGrammar;
import com.niton.parser.grammar.types.MultiGrammar;
import com.niton.parser.token.GenericToken;
import com.niton.parser.token.TokenPattern;
import com.niton.parser.token.Tokenable;
import com.niton.parser.token.Tokenizer;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.niton.parsers.grammar.GrammarFileGrammar.GRAMMAR_FILE_GRAMMAR;
import static com.niton.parsers.grammar.GrammarFileGrammar.Property.*;
import static com.niton.parsers.grammar.GrammarFileGrammarName.*;
import static com.niton.parsers.grammar.GrammarFileTokens.*;

/**
 * Parser for GrammarFiles
 *
 * @author Nils Brugger
 * @version 2019-06-09
 */
public class GrammarFileParser extends Parser<GrammarFileContent> {

	public GrammarFileParser() {
		super(GRAMMAR_FILE_GRAMMAR);
		setTokenizer(new Tokenizer(GrammarFileTokens.values()));
	}

	/**
	 * @throws ParsingException
	 * @see com.niton.parser.Parser#convert(AstNode)
	 */
	@Override
	public GrammarFileContent convert(AstNode o) throws ParsingException {
		var root   = o.reduce("GrammarFile");
		var head   = root.getSubNode(HEAD.id());
		var result = new GrammarFileContent();
		var tokens = head.flatMap(node -> node.getSubNode(TOKEN_DEFINERS.id()))
		                 .map(ReducedNode::getChildren)
		                 .stream()
		                 .flatMap(List::stream)
		                 .map(this::parseTokenDefiner)
		                 .collect(Collectors.toSet());
		result.setTokens(tokens);

		root.getSubNode(GRAMMARS.id())
		    .map(ReducedNode::getChildren)
		    .stream()
		    .flatMap(List::stream)
		    .map(this::parseChainGrammar)
		    .forEach(result.getGrammars()::map);
		return result;
	}

	private ChainGrammar parseChainGrammar(ReducedNode rootGrammarNode) {
		var name = rootGrammarNode.getSubNode(NAME.id())
		                          .map(ReducedNode::getValue)
		                          .orElseThrow(() -> new RuntimeException("Grammar has no name"));
		var chain = new ChainGrammar();
		chain.setName(name);
		rootGrammarNode.getSubNode(CHAIN.id())
		               .map(ReducedNode::getChildren)
		               .stream()
		               .flatMap(List::stream)
		               .map(this::parseGenericGrammar)
		               .forEach(grm -> {
						   var propName = grm.getName();
			               chain.addGrammar(grm.named((String) null), propName);
		               });

		return chain;
	}

	private Grammar<?, ?> parseGenericGrammar(ReducedNode grammarDefinition) {
		var operator = grammarDefinition.getSubNode(OPERATION.id());
		var item = grammarDefinition.getSubNode(ITEM.id()).orElseThrow(
				() -> new RuntimeException("No item for grammar")
		);
		var           repeat     = grammarDefinition.getSubNode(REPEAT.id());
		var           assignment = grammarDefinition.getSubNode(ASSIGNMENT.id());
		Grammar<?, ?> grammar    = parseItem(item);
		if(operator.isPresent()){
			var operatorValue = operator.get().getValue();
			if(operatorValue.matches(ANY_EXCEPT_SIGN.regex))
				grammar = grammar.anyExcept();
			else if(operatorValue.matches(OPTIONAL_SIGN.regex))
				grammar = grammar.optional();
			else if(operatorValue.matches(IGNORE_SIGN.regex))
				grammar = grammar.ignore();
		}
		if(repeat.isPresent()){
			grammar = grammar.repeat();
		}
		if(assignment.isPresent()){
			var name = assignment.get().getSubNode(NAME.id()).orElseThrow().getValue();
			grammar = grammar.named(name);
		}
		return grammar;
	}

	private Grammar<?, ?> parseItem(ReducedNode item) {
		var type              = item.getSubNode("type").orElseThrow().getValue();
		var possibleReference = parseReferenceType(item, type);
		if (possibleReference.isPresent())
			return possibleReference.get();
		if (type.equals(ARRAY.name())) {
			return parseArrayGrammar(item.getSubNode("value").orElseThrow());
		} else {
			throw new IllegalArgumentException(type + " is not a known item type");
		}
	}

	private Optional<Grammar<?, ?>> parseReferenceType(ReducedNode item, String type) {
		if (type.equals(TOKEN_REFERENCE.name())) {
			return Optional.of(parseTokenReference(item.getSubNode("value").orElseThrow()));
		}
		if (type.equals(GRAMMAR_REFERENCE.name())) {
			return Optional.of(
					Grammar.reference(item.getSubNode("value").orElseThrow().getValue())
			);
		}
		return Optional.empty();
	}

	private MultiGrammar parseArrayGrammar(ReducedNode value) {
		var refs = value
				.getSubNode(ITEMS.id())
				.map(ReducedNode::getChildren)
				.stream()
				.flatMap(List::stream)
				.map(item -> item.getSubNode(REFERENCE.id()))
				.map(Optional::orElseThrow)
				.map(ref -> {
					var type = ref.getSubNode("type").orElseThrow().getValue();
					return parseReferenceType(ref, type).orElseThrow(
							() -> new IllegalArgumentException(type + " is not a known item type")
					);
				})
				.toArray(Grammar<?,?>[]::new);
		return new MultiGrammar(refs);
	}

	private Grammar<?, ?> parseTokenReference(ReducedNode value) {
		return Grammar.tokenReference(value.getSubNode(TOKEN_NAME.id()).orElseThrow().getValue());
	}

	private Tokenable parseTokenDefiner(ReducedNode reducedNode) {
		var regex = reducedNode.getSubNode(LITERAL.id())
		                       .flatMap(literal -> literal.getSubNode(REGEX.id()))
		                       .map(ReducedNode::getValue)
		                       .map(TokenPattern::new)
		                       .orElseThrow(() -> new RuntimeException("No regex in token"));
		var name = reducedNode.getSubNode(NAME.id())
		                      .map(ReducedNode::getValue)
		                      .orElseThrow(() -> new RuntimeException("No name in token"));
		return new GenericToken(regex, name);
	}


}

