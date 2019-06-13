package com.niton.csvp.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import com.niton.media.ResurceLoader;
import com.niton.media.filesystem.NFile;
import com.niton.parser.DefaultParser;
import com.niton.parser.GrammarObject;
import com.niton.parser.Parser;
import com.niton.parser.ParsingException;
import com.niton.parser.SubGrammerObject;
import com.niton.parser.grammar.exectors.GrammarExecutor;
import com.niton.parser.specific.grammar.GrammarParser;
import com.niton.parser.specific.grammar.GrammarResult;
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
			tokens.add("TOKEN_SIGN");
			tokens.add("OPTIONAL");
			tokens.add("STAR");
			tokens.add("ARROW");
			tokens.add("ARRAY_OPEN");
			tokens.add("ARRAY_CLOSE");
			tokens.add("IGNORE");
			tokens.add("COLON");
			test("LINE_END='\\r\\n'\r\n" + 
					"IDENTIFYER='[A-Za-z_]+'\r\n" + 
					"EQ='[=]'\r\n" + 
					"WHITESPACE='[ \\t]+'\r\n" + 
					"SLASH='\\/'\r\n" + 
					"COMMA=','\r\n"+
					"TOKEN_SIGN='#'\r\n" + 
					"OPTIONAL='\\?'\r\n" + 
					"STAR='\\*'\r\n" + 
					"ARROW='>'\r\n" + 
					"ARRAY_OPEN='\\{'\r\n" + 
					"ARRAY_CLOSE='\\}'\r\n" + 
					"IGNORE='~'\r\n" + 
					"COLON=':'", false, tokens, new ArrayList<>());
	}
	
	@Test
	void parseTokensAndGrammars() {
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
		
		ArrayList<String> grammars = new ArrayList<>();
		grammars.add("whiteignore");
		grammars.add("comment");
		
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
				"BACKSLASH='\\\\(?!\\')'\r\n"+
				"whiteignore:\r\n" + 
				"	{#WHITESPACE, #LINE_END}\r\n" + 
				"\r\n" + 
				"comment: \r\n" + 
				"	#SLASH\r\n" + 
				"	#SLASH\r\n" + 
				"	*#LINE_END >text", false, tokens, grammars);
	}
	
	@Test
	void parseClearGrammars() {
		ArrayList<String> tokens = new ArrayList<>();
		
		ArrayList<String> grammars = new ArrayList<>();
		grammars.add("whiteignore");
		grammars.add("comment");
		
		test(
			"whiteignore:\r\n" + 
			"	{#WHITESPACE, #LINE_END}\r\n" + 
			"\r\n" + 
			"comment: \r\n" + 
			"	#SLASH\r\n" + 
			"	#SLASH\r\n" + 
			"	*#LINE_END >text", true, tokens, grammars);
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
	@Test
	void parseFile() {
		assertDoesNotThrow(()->{
			GrammarParser parser = new GrammarParser();
			byte[] file = ResurceLoader.readOutOfJarFile("resources/Grammar.gmr");
			GrammarResult result = parser.parse(file);
			DefaultParser parsedParser = new DefaultParser(result.grammars, "grammarFile");
			parsedParser.getT().tokens = result.getTokens();
			parsedParser.parse(file);
		});
	}
	
	
	
	private static void test(String s,boolean debug,ArrayList<String> tokenNamesExpected,ArrayList<String> expectedGrammarNames) {
		assertDoesNotThrow(()->{
			Parser p = new GrammarParser();
			if(debug) {
				System.out.println("Tokens : \n"+p.getT().parse(s));
				System.out.println("Grammar Pure :\n "+((SubGrammerObject)p.parsePlain(s)).toString(3));
			}
			GrammarObject result =  p.parsePlain(s);
			GrammarFile file = new GrammarFile((SubGrammerObject)result);
			if(file.getHead() != null && file.getHead().getTokenDefiners() != null) {
				for (IgnoringTokenDefiner string : file.getHead().getTokenDefiners()) {
					assertTrue(tokenNamesExpected.contains(string.getDefiner().getName()),"There was an unexpected element in the parsed token list");
					tokenNamesExpected.remove(string.getDefiner().getName());
				}
			}
			assertEquals(0, tokenNamesExpected.size(),"Some tokens where not parsed "+tokenNamesExpected+"");
			if(file.getGrammars() != null) {
				for (com.niton.parser.specific.grammar.gen.Grammar string : file.getGrammars()) {
					assertTrue(expectedGrammarNames.contains(string.getName()),"There was an unexpected element in the parsed grammar list ("+string.getName()+")");
					expectedGrammarNames.remove(string.getName());
				}
			}
			assertEquals(0, expectedGrammarNames.size(),"Some grammers where not parsed ("+expectedGrammarNames+")");
			
			GrammarExecutor exec =p.getG().get(p.getRoot()).getExecutor();
			exec.check(p.getT().parse(s), p.getG());
			assertEquals(p.getT().parse(s).size() ,exec.index());
		},"An exception ocoured on parsing");
	}
}

