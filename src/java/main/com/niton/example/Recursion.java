package com.niton.example;

import java.io.IOException;

import com.niton.JPGenerator;
import com.niton.parser.GrammarReference;
import com.niton.parser.GrammarReferenceMap;
import com.niton.parser.Parser;
import com.niton.parser.ParsingException;
import com.niton.parser.Tokens;
import com.niton.parser.grammar.Grammar;
import com.niton.parser.grammar.GrammarMatchGrammer;

/**
 * This is the Recursion Class
 * @author Nils Brugger
 * @version 2019-06-07
 */
public class Recursion {

	/**
	 * <b>Description :</b><br>
	 * 
	 * @author Nils Brugger
	 * @version 2019-06-07
	 * @param args
	 * @throws IOException 
	 * @throws ParsingException 
	 */
	public static void main(String[] args) throws IOException, ParsingException {
		GrammarReference ref = new GrammarReferenceMap()
				.map(Grammar.build("Number").matchToken(Tokens.NUMBER))
				.map(Grammar.build("calc_expression").matchToken(Tokens.BRACKET_OPEN).match("expression","firstExpression").matchAnyToken("calculationType",Tokens.MULTIPLICATOR,Tokens.PLUS,Tokens.MINUS,Tokens.SLASH).match("expression","secondExpression").matchToken(Tokens.BRACKET_CLOSED))
				.map(Grammar.build("expression").matchAny("matched", new String[]{"Number","calc_expression"}));
		String s = "((((1+4)/5)*22)-99)";
//		JPGenerator gen = new JPGenerator("com.niton.generated", "D:\\Users\\Nils\\Desktop\\Workspaces\\API\\JainParse\\src\\java\\main");
//		gen.generate("expression", ref);
		Parser p = new Parser(ref, "expression");
		System.out.println(p.parse(s));
	}
}

