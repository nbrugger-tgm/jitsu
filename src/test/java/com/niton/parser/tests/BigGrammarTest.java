package com.niton.parser.tests;

import com.niton.ResultDisplay;
import com.niton.media.filesystem.NFile;
import com.niton.parser.GrammarReferenceMap;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.result.SuperGrammarResult;
import com.niton.parser.specific.grammar.GrammarParser;

import java.io.IOException;

public class BigGrammarTest {
	public static void main(String[] args) throws ParsingException, IOException {
		GrammarReferenceMap map    = new GrammarReferenceMap();
		GrammarParser       parser = new GrammarParser();
		String              txt    = new NFile(
				"D:\\Users\\Nils\\Desktop\\Workspaces\\Programme\\Nevermind\\resources\\basics.grm")
				.getText();

		ResultDisplay dspl = new ResultDisplay((SuperGrammarResult) parser.parsePlain(txt), map);
		dspl.setTokenList(parser.getTokenizer().tokenize(txt));
		dspl.display();
	}
}
