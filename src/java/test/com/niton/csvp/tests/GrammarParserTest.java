package com.niton.csvp.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import com.niton.parser.Parser;
import com.niton.parser.ParsingException;
import com.niton.parser.SubGrammerObject;
import com.niton.parser.specific.grammar.GrammarParser;
import com.niton.parser.specific.grammar.gen.GrammarFile;
import com.niton.parser.specific.grammar.gen.IgnoringTokenDefiner;

/**
 * This is the GrammarParserTest Class
 * @author Nils Brugger
 * @version 2019-06-12
 */
class GrammarParserTest {
	public GrammarParserTest() {

	}

	@Test
	void parseWellTokens() {
			ArrayList<String> tokens = new ArrayList<>();
			tokens.add("LINE_END");
			tokens.add("IDENTIFYER");
			tokens.add("EQ");
			tokens.add("WHITESPACE");
			tokens.add("SLASH");
			tokens.add("COMMA");
			test("LINE_END='\\r\\n'\r\n" + 
					"IDENTIFYER='[A-Za-z_]+'\r\n" + 
					"EQ='[=]'\r\n" + 
					"WHITESPACE='[ \\t]+'\r\n" + 
					"SLASH='\\/'\r\n" + 
					"COMMA=','", false, tokens, new ArrayList<>());
	}
	
	@Test
	void parseTokensWithComments() {
		ArrayList<String> tokens = new ArrayList<>();
		tokens.add("LINE_END");
		tokens.add("IDENTIFYER");
		tokens.add("EQ");
		tokens.add("WHITESPACE");
		tokens.add("SLASH");
		tokens.add("COMMA");
		tokens.add("QUOTE");
		tokens.add("ESCAPED_QUOTE");
		tokens.add("BACKSLASH");
		test("//General\r\n" + 
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
				"BACKSLASH='\\\\(?!\\')'", false, tokens, new ArrayList<>());
	}
	
	private static void test(String s,boolean debug,ArrayList<String> tokenNamesExpected,ArrayList<String> expectedGrammarNames) {
		assertDoesNotThrow(()->{
			Parser p = new GrammarParser();
			if(debug) {
				System.out.println("\tTokens : \n"+p.getT().parse(s));
				System.out.println("\tGrammar Pure : "+((SubGrammerObject)p.parse(s)).toString(3));
			}
			GrammarFile file = new GrammarFile((SubGrammerObject) p.parse(s));
			if(file.getHead() != null && file.getHead().getTokenDefiners() != null) {
				for (IgnoringTokenDefiner string : file.getHead().getTokenDefiners()) {
					assertTrue(tokenNamesExpected.contains(string.getDefiner().getName()),"There was an unexpected element in the parsed list");
					tokenNamesExpected.remove(string.getDefiner().getName());
				}
			}
			assertEquals(0, tokenNamesExpected.size(),"Some tokens where not parsed "+tokenNamesExpected+"");
			if(file.getGrammars() != null) {
				for (com.niton.parser.specific.grammar.gen.Grammar string : file.getGrammars()) {
					assertTrue(expectedGrammarNames.contains(string.getName()),"There was an unexpected element in the parsed list");
					expectedGrammarNames.remove(string.getName());
				}
			}
			assertEquals(0, expectedGrammarNames.size(),"Some grammers where not parsed");
		},"An exception ocoured on parsing");
	}
}

