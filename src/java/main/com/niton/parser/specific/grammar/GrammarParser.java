package com.niton.parser.specific.grammar;

import java.io.IOException;

import com.niton.JPGenerator;
import com.niton.parser.GrammarReference;
import com.niton.parser.GrammarReferenceMap;
import com.niton.parser.Parser;
import com.niton.parser.ParsingException;
import com.niton.parser.Token;
import com.niton.parser.grammar.Grammar;

/**
 * This is the GrammarParser Class
 * @author Nils Brugger
 * @version 2019-06-09
 */
public class GrammarParser extends Parser {
	public static GrammarReference gram = new GrammarReferenceMap()
		.map(
			Grammar.build("whiteignore")
			.matchAnyToken(GrammarTokens.WHITESPACE,GrammarTokens.LINE_END)
		)
		.map(
			Grammar.build("comment")
			.matchToken(GrammarTokens.SLASH)
			.matchToken(GrammarTokens.SLASH)
			.anyExcept(GrammarTokens.LINE_END,"message")
		)
		.map(Grammar.build("combineignore").matchAny("ignore",new String[] {"comment","whiteignore"}))
		.map(Grammar.build("allignore").repeatMatch("combineignore","ignored"))
		.map(
			Grammar.build("tokenLiteral")
			.matchToken(GrammarTokens.QUOTE)
			.anyExcept(GrammarTokens.QUOTE,"regex")
			.matchToken(GrammarTokens.QUOTE)
		)
		.map(
			Grammar.build("tokenDefiner")
			.matchToken(GrammarTokens.IDENTIFYER, "name")
			.ignoreToken(GrammarTokens.WHITESPACE)
			.matchToken(GrammarTokens.EQ)
			.ignoreToken(GrammarTokens.WHITESPACE)
			.match("tokenLiteral", "literal")
			.matchTokenOptional(GrammarTokens.LINE_END)
		)
		.map(
				Grammar.build("ignoringTokenDefiner")
				.ignore("allignore")
				.match("tokenDefiner", "definer")
		)
		.map(
				Grammar.build("fileHead")
				.repeatMatch("ignoringTokenDefiner","tokenDefiners")
		)
		.map(
				Grammar.build("grammarLiteral")
				.matchToken(GrammarTokens.IDENTIFYER,"name")
		)
		.map(
			Grammar.build("tokenSubst")
			.matchToken(GrammarTokens.TOKEN_SIGN)
			.matchToken(GrammarTokens.IDENTIFYER, "tokenName")
		)
		.map(
				Grammar.build("nameAssignment")
				.ignoreToken(GrammarTokens.WHITESPACE)
				.matchToken(GrammarTokens.ARROW)
				.ignoreToken(GrammarTokens.WHITESPACE)
				.matchToken(GrammarTokens.IDENTIFYER,"name")
				
		)
		
	
		.map(
			Grammar.build("matchOperation")
			.ignoreToken(GrammarTokens.WHITESPACE)
			.matchTokenOptional(GrammarTokens.STAR,"anyExcept")
			.ignoreToken(GrammarTokens.WHITESPACE)
			.matchTokenOptional(GrammarTokens.OPTIONAL,"optional")
			.ignoreToken(GrammarTokens.WHITESPACE)
			.matchTokenOptional(GrammarTokens.IGNORE,"ignore")
			.ignoreToken(GrammarTokens.WHITESPACE)
			.matchAny("check", new String[] {"tokenSubst","grammarLiteral"})
			.ignoreToken(GrammarTokens.WHITESPACE)
			.matchTokenOptional(GrammarTokens.STAR,"repeat")
			.ignoreToken(GrammarTokens.WHITESPACE)
			.matchOptional("nameAssignment", "assignment")
			.ignoreToken(GrammarTokens.WHITESPACE)
			.matchToken(GrammarTokens.LINE_END)
			
		)
		.map(
			Grammar.build("arrayItem")
			.matchAny("item",new String[] {"tokenSubst","grammarLiteral"})
			.ignoreToken(GrammarTokens.WHITESPACE)
			.ignoreToken(GrammarTokens.COMMA)
			.ignoreToken(GrammarTokens.WHITESPACE)
		)
		.map(
				Grammar.build("orOperation")
				.ignoreToken(GrammarTokens.WHITESPACE)
				.matchToken(GrammarTokens.ARRAY_OPEN)
				.repeatMatch("arrayItem", "items")
				.matchToken(GrammarTokens.ARRAY_CLOSE)
				.ignoreToken(GrammarTokens.WHITESPACE)
				.matchToken(GrammarTokens.LINE_END)
		)
		.map(
				Grammar.build("rule")
				.matchAny("operation", new String[] {"orOperation","matchOperation"})
		)
		.map(
				Grammar.build("grammar")
				.matchToken(GrammarTokens.IDENTIFYER,"name")
				.ignoreToken(GrammarTokens.WHITESPACE)
				.matchToken(GrammarTokens.COLON)
				.ignoreToken(GrammarTokens.WHITESPACE)
				.matchToken(GrammarTokens.LINE_END)
				.repeatMatch("rule", "chain")
		)
		.map(
				Grammar.build("grammarFile")
				.match("fileHead", "head")
				.repeatMatch("grammar","grammars")
		);
	public GrammarParser() {
		super(gram, "grammarFile");
		getT().tokens.clear();
		for (GrammarTokens elem : GrammarTokens.values()) {
			getT().tokens.put(elem.name(), new Token(elem.regex));
		}
	}
	public static void main(String[] args) throws IOException, ParsingException {
		JPGenerator gen = new JPGenerator("com.niton.parser.specific.grammar.gen", "D:\\Users\\Nils\\Desktop\\Workspaces\\API\\JainParse\\src\\java\\main");
		gen.generate(gram);
	}
	
}

