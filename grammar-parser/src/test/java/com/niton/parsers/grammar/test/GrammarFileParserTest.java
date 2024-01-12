package com.niton.parsers.grammar.test;

import com.niton.parser.DefaultParser;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammar.api.GrammarReferenceMap;
import com.niton.parser.grammar.matchers.ReferenceGrammarMatcher;
import com.niton.parser.token.Tokenable;
import com.niton.parser.token.Tokenizer;
import com.niton.parsers.grammar.GrammarFileGrammar;
import com.niton.parsers.grammar.GrammarFileParser;
import com.niton.parsers.grammar.GrammarFileTokens;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * This is the GrammarParserTest Class
 *
 * @author Nils Brugger
 * @version 2019-06-12
 */
class GrammarFileParserTest {
	public GrammarFileParserTest() {

	}

	private static void test(
			String grammarFileContent,
			Set<String> tokenNamesExpected,
			Set<String> expectedGrammarNames
	) {
		assertDoesNotThrow(() -> {
			GrammarFileParser grammarFileParser = new GrammarFileParser();
			var               result            = grammarFileParser.parse(grammarFileContent);
			assertThat(result.getTokens())
					.extracting(Tokenable::name)
					.containsAll(tokenNamesExpected);
			assertThat(result.getGrammars().grammarNames())
					.containsAll(expectedGrammarNames);
		});
	}

	@Test
	void parseWellTokens() {
		Set<String> tokens = Set.of(
				"LINE_END",
				"IDENTIFYER",
				"EQ",
				"WHITESPACE",
				"SLASH",
				"COMMA",
				"TOKEN_SIGN",
				"OPTIONAL",
				"STAR",
				"ARROW",
				"ARRAY_OPEN",
				"ARRAY_CLOSE",
				"IGNORE",
				"COLON"
		);
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
				     "COLON=':'", tokens, Set.of());
	}

	@Test
	void parseTokensAndGrammars() {
		Set<String> tokens = Set.of(
				"LINE_END",
				"IDENTIFYER",
				"EQ",
				"WHITESPACE",
				"SLASH",
				"COMMA",
				"QUOTE",
				"ESCAPED_QUOTE",
				"BACKSLASH"
		);

		Set<String> grammars = Set.of("whiteignore", "comment");

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
				     "	!#LINE_END >text", tokens, grammars);
	}

	@Test
	void parseClearGrammars() {
		test(
				"whiteignore:\r\n" +
						"	{#WHITESPACE, #LINE_END}\r\n" +
						"\r\n" +
						"comment: \r\n" +
						"	#SLASH\r\n" +
						"	#SLASH\r\n" +
						"	!#LINE_END >text", Set.of(), Set.of("whiteignore", "comment")
		);
	}
	@Test
	void parseCombinedGrammar() {
		test(
				"whiteignore:\r\n" +
						"	?{#WHITESPACE, #LINE_END}*\r\n" +
						"\r\n" +
						"comment: \r\n" +
						"	~SLASH\r\n" +
						"	!#LINE_END >text", Set.of(), Set.of("whiteignore", "comment")
		);
	}

	@Test
	void parseTokensWithComments() {
		var tokens = Set.of(
				"LINE_END",
				"IDENTIFYER",
				"EQ",
				"WHITESPACE",
				"SLASH",
				"COMMA",
				"QUOTE",
				"ESCAPED_QUOTE",
				"BACKSLASH"
		);
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
				     "BACKSLASH='\\\\(?!\\')'", tokens, Set.of());
	}

	public static void main(String[] args)  {
		DefaultParser p = new DefaultParser(
				new GrammarReferenceMap().deepMap(GrammarFileGrammar.GRAMMAR_FILE_GRAMMAR),
				GrammarFileGrammar.GRAMMAR_FILE_GRAMMAR
		);
		p.setTokenizer(new Tokenizer(GrammarFileTokens.values()));
		System.out.println(p.parse(
				"STRING_DELMITTER = '\"'\n" +
						"IDENTIFYER = '[A-Za-z_]+'\n" +
						"WHITESPACE = '[ \\t]+'\n" +
						"EQUAL = '='\n" +
						"SEMICOLON = ';'\n" +
						"\n" +
						"String:\n" +
						"    #STRING_DELMITTER\n" +
						"    !#STRING_DELMITTER >content\n" +
						"    #STRING_DELMITTER\n" +
						"\n" +
						"VariableAssignment:\n" +
						"    #IDENTIFYER > name\n" +
						"    ~#WHITESPACE\n" +
						"    #EQUAL\n" +
						"    String > value\n" +
						"    #SEMICOLON\n"
		).reduce("GRAMMAR_FILE").get().format());
	}
}

