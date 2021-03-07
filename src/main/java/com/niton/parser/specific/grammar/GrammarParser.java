package com.niton.parser.specific.grammar;

import com.niton.JPGenerator;
import com.niton.parser.*;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammars.ChainGrammar;
import com.niton.parser.result.SuperGrammarResult;
import com.niton.parser.specific.grammar.gen.TokenReference;
import com.niton.parser.specific.grammar.gen.*;

import java.io.IOException;

import static com.niton.parser.specific.grammar.GrammarGrammarName.*;
import static com.niton.parser.specific.grammar.GrammarTokens.*;

/**
 * Parser for GrammarFiles
 *
 * @author Nils Brugger
 * @version 2019-06-09
 */
public class GrammarParser extends Parser<GrammarFileContent> {

	public static ChainGrammar
			whitespace           = Grammar.build(WHITESPACE)
			                              .tokens(SPACE, LINE_END).add(),
			comment              = Grammar.build(COMMENT)
			                              .token(SLASH).add()
			                              .token(SLASH).add()
			                              .token(LINE_END).anyExcept().add("message"),
			toIgnore             = Grammar.build(TO_IGNORE)
			                              .grammars(comment, whitespace).add("ignored"),
			repeatIgnore         = Grammar.build(GrammarGrammarName.REPEAT_IGNORE)
			                              .grammar(toIgnore).repeat().add("ignored"),
			tokenLiteral         = Grammar.build(TOKEN_LITERAL)
			                              .token(QUOTE).add()
			                              .token(QUOTE).anyExcept().add("regex")
			                              .token(QUOTE).add(),
			tokenDefiner         = Grammar.build(TOKEN_DEFINER)
			                              .token(IDENTIFYER).add("name")
			                              .token(SPACE).ignore().add()
			                              .token(EQ).add()
			                              .token(SPACE).ignore().add()
			                              .grammar(tokenLiteral).add("literal")
			                              .tokens(LINE_END, EOF).add(),
			ignoringTokenDefiner = Grammar.build(IGNORING_TOKEN_DEFINER)
			                              .grammar(repeatIgnore).ignore().add()
			                              .grammar(tokenDefiner).add("content"),
			fileHead             = Grammar.build(FILE_HEAD)
			                              .grammar(ignoringTokenDefiner)
			                              .repeat()
			                              .add("token_definers"),
			grammarReference     = Grammar.build(GRAMMAR_REFERENCE)
			                              .token(IDENTIFYER).add("grammar_name"),
			tokenReference       = Grammar.build(TOKEN_REFERENCE)
			                              .token(TOKEN_SIGN).add()
			                              .token(IDENTIFYER).add("token_name"),
			nameAssignment       = Grammar.build(NAME_ASSIGNMENT)
			                              .token(SPACE).ignore().add()
			                              .token(ARROW).add()
			                              .token(SPACE).ignore().add()
			                              .token(IDENTIFYER).add("name"),
			arrayItem            =
					Grammar.build(ARRAY_ITEM)
					       .grammars(tokenReference, grammarReference).add("item")
					       .tokens(SPACE, COMMA).optional().add("seperator")
					       .token(SPACE).repeat().ignore().add(),
			array                = Grammar.build(ARRAY)
			                              .token(SPACE).ignore().add()
			                              .token(ARRAY_OPEN).add()
			                              .grammar(arrayItem).repeat().add("items")
			                              .token(ARRAY_CLOSE).add()
			                              .token(SPACE).ignore().add(),
			subGrammar           =
					Grammar.build(SUB_GRAMMAR)
					       .token(SPACE)
					       .ignore()
					       .add()
					       .tokens(ANY_EXCEPT_SIGN, OPTIONAL_SIGN, IGNORE_SIGN)
					       .optional()
					       .add("operation")
					       .grammars(tokenReference, grammarReference, array)
					       .add("item")
					       .token(SPACE)
					       .ignore()
					       .add()
					       .token(REPEAT_SIGN)
					       .optional()
					       .add("repeat")
					       .token(SPACE)
					       .ignore()
					       .add()
					       .grammar(nameAssignment)
					       .optional()
					       .add("assignment")
					       .tokens(LINE_END, EOF)
					       .add(),
			rootGrammar          = Grammar.build(ROOT_GRAMMAR)
			                              .grammar(repeatIgnore).ignore().add()
			                              .token(IDENTIFYER).add("name")
			                              .token(SPACE).ignore().add()
			                              .token(COLON).add()
			                              .token(SPACE).ignore().add()
			                              .tokens(LINE_END, EOF).add()
			                              .grammar(subGrammar).repeat().name("chain"),
			grammarFile          = Grammar.build(GRAMMAR_FILE)
			                              .grammar(fileHead).add("head")
			                              .grammar(rootGrammar).repeat().add("grammars");


	public GrammarParser() {
		super(grammarFile);
		getTokenizer().tokens.clear();
		for (GrammarTokens elem : GrammarTokens.values()) {
			getTokenizer().tokens.put(elem.name(), new Token(elem.regex));
		}
	}

	/**
	 * Autogenerates the parse model for a Grammar
	 *
	 * @param args
	 * @throws IOException
	 * @throws ParsingException
	 */
	public static void main(String[] args) throws IOException, ParsingException {
		JPGenerator gen = new JPGenerator("com.niton.parser.specific.grammar.gen",
		                                  "D:\\Users\\Nils\\Desktop\\Workspaces\\libs\\JainParse\\src\\main\\java");
		gen.generate(new GrammarReferenceMap().deepMap(grammarFile), GrammarTokens.values());
	}

	/**
	 * @throws ParsingException
	 * @see com.niton.parser.Parser#convert(GrammarResult)
	 */
	@Override
	public GrammarFileContent convert(GrammarResult o) throws ParsingException {
		GrammarFile        g      = new GrammarFile((SuperGrammarResult) o);
		GrammarFileContent result = new GrammarFileContent();
		for (IgnoringTokenDefiner definer : g.getHead().getTokenDefiners()) {
			result.getTokens()
			      .put(definer.getContent().getName(),
			           new Token(definer.getContent()
			                            .getLiteral()
			                            .getRegex()
			                            .replaceAll("\\\\'", "'")));
		}
		for (RootGrammar gram : g.getGrammars()) {
			ChainGrammar gr = Grammar.build(gram.getName());
			for (SubGrammar r : gram.getChain()) {
				String name      = null;
				String operation = r.getOperation();
                if (r.getAssignment() != null) {
                    name = r.getAssignment().getName();
                }

				Grammar chainElement;

				//Item parsing
				if (r.getItem().getType().equals(GRAMMAR_REFERENCE.getName())) {
					com.niton.parser.specific.grammar.gen.GrammarReference check       = new com.niton.parser.specific.grammar.gen.GrammarReference(
							(SuperGrammarResult) r.getItem().getRes());
					String                                                 grammarName = check.getGrammarName();
					chainElement = Grammar.reference(grammarName);
				} else if (r.getItem().getType().equals(TOKEN_REFERENCE.getName())) {
					TokenReference toksub = new TokenReference((SuperGrammarResult) r.getItem()
					                                                                 .getRes());
					String         token  = toksub.getTokenName();
					chainElement = Grammar.tokenReference(token);
				} else if (r.getItem().getType().equals(ARRAY.getName())) {
					Array arr = new Array((SuperGrammarResult) r.getItem().getRes());
					if (arr.getItems().size() == 0) {
						throw new ParsingException(
								"Empty arrays are not allowed (and they don't make any sense)");
					} else if (arr.getItems()
					              .get(0)
					              .getItem()
					              .getType()
					              .equals(GRAMMAR_REFERENCE.getName())) {
						Grammar[] grms = new Grammar[arr.getItems().size()];
						for (int i = 0; i < grms.length; i++) {
                            if (arr.getItems()
                                   .get(i)
                                   .getItem()
                                   .getType()
                                   .equals(TOKEN_REFERENCE.getName())) {
                                throw new ParsingException(
                                        "It is not allowed to mix Grammars and tokens in arrays");
                            }
							grms[i] = Grammar.reference(new com.niton.parser.specific.grammar.gen.GrammarReference(
									(SuperGrammarResult) arr.getItems()
									                        .get(i)
									                        .getItem()
									                        .getRes()).getGrammarName());
						}
						chainElement = Grammar.anyOf(grms);
					} else if (arr.getItems()
					              .get(0)
					              .getItem()
					              .getType()
					              .equals(TOKEN_REFERENCE.getName())) {
						Grammar[] grms = new Grammar[arr.getItems().size()];
						for (int i = 0; i < grms.length; i++) {
                            if (arr.getItems()
                                   .get(i)
                                   .getItem()
                                   .getType()
                                   .equals(GRAMMAR_REFERENCE.getName())) {
                                throw new ParsingException(
                                        "It is not allowed to mix Grammars and tokens in arrays");
                            }
							grms[i] = Grammar.tokenReference(new com.niton.parser.specific.grammar.gen.TokenReference(
									(SuperGrammarResult) arr.getItems()
									                        .get(i)
									                        .getItem()
									                        .getRes()).getTokenName());
						}
						chainElement = Grammar.anyOf(grms);
					} else {
						throw new ParsingException("The item type \"" + arr.getItems()
						                                                   .get(0)
						                                                   .getItem()
						                                                   .getType() + "\" is an unkown type to us!");
					}
				} else {
					throw new ParsingException("The item type \"" + r.getItem()
					                                                 .getType() + "\" is an unkown type to us!");
				}

				//apply operations
                if (operation != null) {
                    if (operation.matches(ANY_EXCEPT_SIGN.pattern())) {
                        chainElement = Grammar.anyExcept(chainElement);
                    } else if (operation.matches(IGNORE_SIGN.pattern())) {
                        chainElement = Grammar.ignore(chainElement);
                    } else if (operation.matches(OPTIONAL_SIGN.pattern())) {
                        chainElement = Grammar.optional(chainElement);
                    }
                }

				//apply repeat
                if (r.getRepeat() != null) {
                    chainElement = Grammar.repeat(chainElement);
                }

				gr.grammar(chainElement).add(name);
			}
			result.getGrammars().map(gr);
		}
		return result;
	}

}

