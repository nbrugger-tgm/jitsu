package com.niton.parser.tests;

import com.niton.parser.DefaultParser;
import com.niton.parser.Grammar;
import com.niton.parser.GrammarReferenceMap;
import com.niton.parser.exceptions.ParsingException;

import static com.niton.parser.token.DefaultToken.*;

public class AlreadyCheckTest {
	public static void main(String[] args) throws ParsingException {
		GrammarReferenceMap map = new GrammarReferenceMap()
				.map(
						Grammar
								.build("ignore")
								.tokens(WHITESPACE, NEW_LINE).add()
				)
				.map(
						Grammar
								.build("block")
								.grammar("ignore").ignore().add()
								.token(ROUND_BRACKET_OPEN).add()
								.grammar("ignore").ignore().add()
								.token(NEW_LINE).add()
								.token(ROUND_BRACKET_CLOSED).anyExcept().add()
								.token(ROUND_BRACKET_CLOSED).add()
				);
		String        s      = "\t\t   {\ngutigutgztuzuz}";
		DefaultParser parser = new DefaultParser(map, "block");
		parser.parsePlain(s);
	}
}
