package com.niton.parser.specific.grammar;

import java.io.IOException;

import com.niton.JPGenerator;
import com.niton.parser.GrammarObject;
import com.niton.parser.GrammarReference;
import com.niton.parser.GrammarReferenceMap;
import com.niton.parser.Parser;
import com.niton.parser.ParsingException;
import com.niton.parser.SubGrammerObject;
import com.niton.parser.Token;
import com.niton.parser.grammar.ChainGrammer;
import com.niton.parser.grammar.Grammar;
import com.niton.parser.specific.grammar.gen.GrammarFile;
import com.niton.parser.specific.grammar.gen.GrammarLiteral;
import com.niton.parser.specific.grammar.gen.IgnoringTokenDefiner;
import com.niton.parser.specific.grammar.gen.MatchOperation;
import com.niton.parser.specific.grammar.gen.Rule;
import com.niton.parser.specific.grammar.gen.TokenSubst;

/**
 * This is the GrammarParser Class
 * @author Nils Brugger
 * @version 2019-06-09
 */
public class GrammarParser extends Parser<GrammarResult> {
	
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
			.matchAnyToken(GrammarTokens.LINE_END,GrammarTokens.EOF)
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
			.matchToken(GrammarTokens.IDENTIFYER,"grammarName")
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
			.matchToken(GrammarTokens.ARRAY_OPEN)
			.repeatMatch("arrayItem", "items")
			.matchToken(GrammarTokens.ARRAY_CLOSE)
		)
		.map(
			Grammar.build("rule")
			.ignoreToken(GrammarTokens.WHITESPACE)
			.matchAny("operation", new String[] {"orOperation","matchOperation"})
			.ignoreToken(GrammarTokens.WHITESPACE)
			.matchAnyToken(GrammarTokens.LINE_END,GrammarTokens.EOF)
		)
		.map(
			Grammar.build("grammar")
			.ignore("allignore")
			.matchToken(GrammarTokens.IDENTIFYER,"name")
			.ignoreToken(GrammarTokens.WHITESPACE)
			.matchToken(GrammarTokens.COLON)
			.ignoreToken(GrammarTokens.WHITESPACE)
			.matchAnyToken(GrammarTokens.LINE_END,GrammarTokens.EOF)
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
	/**
	 * @throws ParsingException 
	 * @see com.niton.parser.Parser#convert(com.niton.parser.GrammarObject)
	 */
	@Override
	public GrammarResult convert(GrammarObject o) throws ParsingException {
		GrammarFile g = new GrammarFile((SubGrammerObject) o);
		GrammarResult result = new GrammarResult();
		for (IgnoringTokenDefiner definer : g.getHead().getTokenDefiners()) {
			result.getTokens().put(definer.getDefiner().getName(), new Token(definer.getDefiner().getLiteral().getRegex().replaceAll("\\\\'", "'")));

		}
		for (com.niton.parser.specific.grammar.gen.Grammar gram : g.getGrammars()) {
			ChainGrammer gr = Grammar.build(gram.getName());
			int line = 0;
			for (Rule r : gram.getChain()) {
				line ++;
				if(r.getOperation().getName().equals("matchOperation")) {
					MatchOperation op = new MatchOperation((SubGrammerObject) r.getOperation());
					String name = null;
					if(op.getAssignment() != null)
						name = op.getAssignment().getName();
					if(op.getCheck().getName().equals("grammarLiteral")) {
						//GRAMMAR
						GrammarLiteral check = new GrammarLiteral((SubGrammerObject) op.getCheck());
						String grammarName = check.getGrammarName();
						if(op.getAnyExcept() != null)
							throw new ParsingException("Any Except Operator is not valid for Grammars (can only be used for tokens) ["+gram.getName()+" line "+line+"]");
						else if(op.getIgnore() != null)
							gr.ignore(grammarName);
						else if(op.getOptional() != null)
							gr.matchOptional(grammarName, name);
						else if(op.getRepeat() != null)
							gr.repeatMatch(grammarName, name);
						else
							gr.match(grammarName,name);
					}else if(op.getCheck().getName().equals("tokenSubst")) {
						TokenSubst toksub = new TokenSubst((SubGrammerObject) op.getCheck());
						String token = toksub.getTokenName();
						if(op.getAnyExcept() != null)
							gr.anyExcept(token, name);
						else if(op.getIgnore() != null)
							gr.ignoreToken(token);
						else if(op.getOptional() != null)
							gr.matchTokenOptional(token, name);
						else if(op.getRepeat() != null)
							gr.repeatMatchToken(token, name);
						else
							gr.matchToken(token,name);
					}else {
						throw new ParsingException("The check \""+op.getCheck().getName()+"\" is an unkown type to us!");
					}
				}
			}
			result.getGrammars().map(gr);
		}
		return result;
	}
	
}

