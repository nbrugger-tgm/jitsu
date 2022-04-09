import com.niton.parser.ast.AstNode;
import com.niton.parser.Parser;
import com.niton.parser.ast.SuperNode;
import com.niton.parsers.grammar.gen.GrammarFile;
import com.niton.parsers.grammar.gen.IgnoringTokenDefiner;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This is the GrammarParserTest Class
 *
 * @author Nils Brugger
 * @version 2019-06-12
 */
class GrammarParserTest {
	public GrammarParserTest() {

	}

	private static void test(String s,
	                         boolean debug,
	                         ArrayList<String> tokenNamesExpected,
	                         ArrayList<String> expectedGrammarNames) {
		Assertions.assertDoesNotThrow(() -> {
			Parser<GrammarFileContent> p = new GrammarParser();
			if (debug) {
				System.out.println("Tokens : \n" + p.getTokenizer().tokenize(s));
				System.out.println("Grammar Pure :\n " + ((SuperNode) p.parsePlain(s)).toString(
						3));
			}
			AstNode     result = p.parsePlain(s);
			GrammarFile file   = new GrammarFile((SuperNode) result);
			if (file.getHead() != null && file.getHead().getTokenDefiners() != null) {
				for (IgnoringTokenDefiner string : file.getHead().getTokenDefiners()) {
					Assertions.assertTrue(tokenNamesExpected.contains(string.getContent().getName()),
					                      "There was an unexpected element in the parsed token list");
					tokenNamesExpected.remove(string.getContent().getName());
				}
			}
			Assertions.assertEquals(0,
			                        tokenNamesExpected.size(),
			                        "Some tokens where not parsed " + tokenNamesExpected + "");
			if (file.getGrammars() != null) {
				for (com.niton.parsers.grammar.gen.RootGrammar string : file.getGrammars()) {
					assertTrue(expectedGrammarNames.contains(string.getName()),
					           "There was an unexpected element in the parsed grammar list (" + string
							           .getName() + ")");
					expectedGrammarNames.remove(string.getName());
				}
			}
			Assertions.assertEquals(0,
			                        expectedGrammarNames.size(),
			                        "Some grammars where not parsed (" + expectedGrammarNames + ")");

			//GrammarMatcher<> exec = p.getReference().get(p.getRoot()).getMatcher();
			//exec.parse(p.getTokenizer().tokenize(s), p.getReference());
			//assertEquals(p.getTokenizer().tokenize(s).size() ,exec.);
		}, "An exception ocoured on parsing");
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
				     "COMMA=','\r\n" +
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
				     "BACKSLASH='\\\\(?!\\')'\r\n" +
				     "whiteignore:\r\n" +
				     "	{#WHITESPACE, #LINE_END}\r\n" +
				     "\r\n" +
				     "comment: \r\n" +
				     "	#SLASH\r\n" +
				     "	#SLASH\r\n" +
				     "	!#LINE_END >text", false, tokens, grammars);
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
						"	!#LINE_END >text", false, tokens, grammars);
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
		Assertions.assertDoesNotThrow(() -> {
			GrammarParser      parser = new GrammarParser();
			byte[]             file   = GrammarParserTest.class.getResourceAsStream("/Example.gmr").readAllBytes();
			GrammarFileContent result = parser.parse(new String(file));
			System.out.println(result.getGrammars().grammarNames());
		});
	}
}

