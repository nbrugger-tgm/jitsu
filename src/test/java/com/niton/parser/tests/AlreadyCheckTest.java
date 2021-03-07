package com.niton.parser.tests;

import com.niton.parser.DefaultParser;
import com.niton.parser.GrammarReferenceMap;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.Grammar;

import static com.niton.parser.token.DefaultToken.*;

public class AlreadyCheckTest {
    public static void main(String[] args) throws ParsingException {
        GrammarReferenceMap map = new GrammarReferenceMap()
                .map(
                        Grammar
                                .build("ignore")
                                .tokens(WHITESPACE, NEW_LINE)
                                .match()
                )
                .map(
                        Grammar
                                .build("block")
                                .grammar("ignore").ignore()
                                .token(ROUND_BRACKET_OPEN).match()
                                .grammar("ignore").ignore()
                                .token(NEW_LINE).match()
                                .token(ROUND_BRACKET_CLOSED).anyExcept()
                                .token(ROUND_BRACKET_CLOSED).match()
                );
        String s = "\t\t   {\ngutigutgztuzuz}";
        DefaultParser parser = new DefaultParser(map, "block");
        parser.parsePlain(s);
    }
}
