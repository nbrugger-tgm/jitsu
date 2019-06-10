package com.niton.parser.specific.grammar;

import java.io.IOException;

import com.niton.JPGenerator;
import com.niton.parser.GrammarReference;
import com.niton.parser.GrammarReferenceMap;
import com.niton.parser.Parser;
import com.niton.parser.ParsingException;
import com.niton.parser.SubGrammerObject;
import com.niton.parser.Token;
import com.niton.parser.grammar.Grammar;
import com.niton.parser.specific.grammar.gen.GrammarFile;
import com.niton.parser.specific.grammar.gen.IgnoringTokenDefiner;

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
		
		
		Parser p = new GrammarParser();
		String s = 
				"//General\r\n" + 
				"LINE_END='\\r\\n'\r\n" + 
				"IDENTIFYER='[A-Za-z_]+'\r\n" + 
				"EQ='[=]'\r\n" + 
				"WHITESPACE='[ \\t]+'\r\n" + 
				"SLASH='\\/'\r\n" + 
				"COMMA=','\r\n" + 
				"\r\n" + 
				"//SOME Escaping shit\r\n" + 
				"QUOTE='(?<!\\\\)\\\\''\r\n" + 
				"ESCAPED_QUOTE='\\\\\\\\''\r\n" + 
				"BACKSLASH='\\\\(?!\\')'\r\n" + 
				"\r\n" + 
				"\r\n" + 
				"\r\n" + 
				"//Specific\r\n" + 
				"TOKEN_SIGN='#'\r\n" + 
				"OPTIONAL='\\?'\r\n" + 
				"STAR='\\*'\r\n" + 
				"ARROW='>'\r\n" + 
				"ARRAY_OPEN='\\{'\r\n" + 
				"ARRAY_CLOSE='\\}'\r\n" + 
				"IGNORE='~'\r\n" + 
				"COLON=':'\r\n" + 
				"\r\n" + 
				"\r\n" + 
				"\r\n" + 
				"\r\n" + 
				"whiteignore:\r\n" + 
				"	{#WHITESPACE, #LINE_END}\r\n" + 
				"\r\n" + 
				"comment: \r\n" + 
				"	#SLASH\r\n" + 
				"	#SLASH\r\n" + 
				"	*#LINE_END >text\r\n" + 
				"\r\n" + 
				"combineignore:\r\n" + 
				"	{whiteignore, comment}\r\n" + 
				"	\r\n" + 
				"allignore:\r\n" + 
				"	combineignore*\r\n" + 
				"	\r\n" + 
				"\r\n" + 
				"\r\n" + 
				"\r\n" + 
				"tokenLiteral:\r\n" + 
				"	#QUOTE\r\n" + 
				"	*#QUOTE >regex\r\n" + 
				"	#QUOTE\r\n" + 
				"	\r\n" + 
				"tokenDefiner: \r\n" + 
				"	comment*\r\n" + 
				"	#IDENTIFYER >name\r\n" + 
				"	~#WHITESPACE\r\n" + 
				"	#EQ \r\n" + 
				"	~#WHITESPACE\r\n" + 
				"	tokenLiteral >literal\r\n" + 
				"	#LINE_END\r\n" + 
				"\r\n" + 
				"ignoringTokenDefiner:\r\n" + 
				"	~allignore\r\n" + 
				"	tokenDefiner >definer\r\n" + 
				"	~allignore\r\n" + 
				"\r\n" + 
				"fileHead:\r\n" + 
				"	ignoringTokenDefiner*\r\n" + 
				"	\r\n" + 
				"	\r\n" + 
				"	\r\n" + 
				"\r\n" + 
				"grammarLiteral:\r\n" + 
				"	#IDENTIFYER\r\n" + 
				"	\r\n" + 
				"tokenSubst:\r\n" + 
				"	#TOKEN_SIGN\r\n" + 
				"	#IDENTIFYER >tokenName\r\n" + 
				"	\r\n" + 
				"nameAssignment\r\n" + 
				"	#ARROW\r\n" + 
				"	~#WHITESPACE\r\n" + 
				"	#IDENTIFYER >givenName\r\n" + 
				"	\r\n" + 
				"matchOperation:\r\n" + 
				"	~#WHITESPACE\r\n" + 
				"	?#STAR >anyExcept\r\n" + 
				"	~#WHITESPACE\r\n" + 
				"	?#OPTIONAL >optional\r\n" + 
				"	~#WHITESPACE\r\n" + 
				"	?#IGNORE >ignore\r\n" + 
				"	~#WHITESPACE\r\n" + 
				"	{tokenSubst, grammarLiteral} >check\r\n" + 
				"	~#WHITESPACE\r\n" + 
				"	?#STAR >repeat\r\n" + 
				"	~#WHITESPACE\r\n" + 
				"	?nameAssignment >nameAssignment\r\n" + 
				"	~#WHITESPACE\r\n" + 
				"	#LINE_END\r\n" + 
				"	\r\n" + 
				"	\r\n" + 
				"arrayItem:\r\n" + 
				"	{tokenSubst, grammarLiteral} > item\r\n" + 
				"	~#WHITESPACE\r\n" + 
				"	~#COMMA\r\n" + 
				"	~#WHITESPACE\r\n" + 
				"	\r\n" + 
				"orOperation:\r\n" + 
				"	~#WHITESPACE\r\n" + 
				"	#ARRAY_OPEN\r\n" + 
				"	arrayItem* > items\r\n" + 
				"	#ARRAY_CLOSE\r\n" + 
				"	~#WHITESPACE\r\n" + 
				"	#LINE_END\r\n" + 
				"	\r\n" + 
				"	\r\n" + 
				"rule:\r\n" + 
				"	{orOperation matchOperation} >operation\r\n" + 
				"\r\n" + 
				"grammar:\r\n" + 
				"	#IDENTIFYER >name\r\n" + 
				"	~#WHITESPACE\r\n" + 
				"	#COLON\r\n" + 
				"	#LINE_END\r\n" + 
				"	rule* >chain\r\n" + 
				"	~allignore\r\n" + 
				"	\r\n" + 
				"grammarFile:\r\n" + 
				"	fileHead >head\r\n" + 
				"	grammar* > grammars\r\n" + 
				"	\r\n" + 
				"	";
		System.out.println(p.getT().parse(s));
		System.out.println(((SubGrammerObject)p.parse(s)).toString(3));
		GrammarFile file = new GrammarFile((SubGrammerObject) p.parse(s));
		System.out.println("Tokens");
		for (IgnoringTokenDefiner string : file.getHead().getTokenDefiners()) {
			System.out.println(string.getDefiner().getName() +" = "+string.getDefiner().getLiteral().getRegex());
		}
		System.out.println("Grammars");
		for (com.niton.parser.specific.grammar.gen.Grammar string : file.getGrammars()) {
			System.out.println(string.getName());
		}
	}
}

