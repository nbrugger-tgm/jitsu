package com.niton.parser.specific.grammar;

import com.niton.JPGenerator;
import com.niton.parser.GrammarReference;
import com.niton.parser.*;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammars.ChainGrammar;
import com.niton.parser.result.SuperGrammarResult;
import com.niton.parser.specific.grammar.gen.TokenReference;
import com.niton.parser.specific.grammar.gen.*;
import com.niton.parser.token.Token;

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

    public static GrammarReference gram = new GrammarReferenceMap()
            .map(
                    Grammar.build(WHITESPACE)
                            .tokens(SPACE, LINE_END).match()
            )
            .map(
                    Grammar.build(COMMENT)
                            .token(SLASH).match()
                            .token(SLASH).match()
                            .token(LINE_END).anyExcept().name("message")
            )
            .map(
                    Grammar.build(TO_IGNORE)
                            .grammars(COMMENT, WHITESPACE).ignore().name("ignored")
            )
            .map(
                    Grammar.build(GrammarGrammarName.REPEAT_IGNORE)
                            .grammar(TO_IGNORE).repeat().name("ignored")
            )
            .map(
                    Grammar.build(TOKEN_LITERAL)
                            .token(QUOTE).match()
                            .token(QUOTE).anyExcept().name("regex")
                            .token(QUOTE).match()
            )
            .map(
                    Grammar.build(TOKEN_DEFINER)
                            .token(IDENTIFYER).match().name("name")
                            .token(SPACE).ignore()
                            .token(EQ).match()
                            .token(SPACE).ignore()
                            .grammar(TOKEN_LITERAL).match().name("literal")
                            .tokens(LINE_END, EOF).match()
            )
            .map(
                    Grammar.build(IGNORING_TOKEN_DEFINER)
                            .grammar(REPEAT_IGNORE).ignore()
                            .grammar(TOKEN_DEFINER).match().name("content")
            )
            .map(
                    Grammar.build(FILE_HEAD)
                            .grammar(IGNORING_TOKEN_DEFINER).repeat().name("token_definers")
            )
            .map(
                    Grammar.build(GRAMMAR_REFERENCE)
                            .token(IDENTIFYER).match().name("grammar_name")
            )
            .map(
                    Grammar.build(TOKEN_REFERENCE)
                            .token(TOKEN_SIGN).match()
                            .token(IDENTIFYER).match().name("token_name")
            )
            .map(
                    Grammar.build(NAME_ASSIGNMENT)
                            .token(SPACE).ignore()
                            .token(ARROW).match()
                            .token(SPACE).ignore()
                            .token(IDENTIFYER).match().name("name")

            )
            .map(
                    Grammar.build(SUB_GRAMMAR)
                            .token(SPACE).ignore()
                            .tokens(ANY_EXCEPT_SIGN, OPTIONAL_SIGN, IGNORE_SIGN).optional().name("operation")
                            .grammars(TOKEN_REFERENCE, GRAMMAR_REFERENCE, ARRAY).match().name("item")
                            .token(SPACE).ignore()
                            .token(REPEAT_SIGN).optional().name("repeat")
                            .token(SPACE).ignore()
                            .grammar(NAME_ASSIGNMENT).optional().name("assignment")
            )
            .map(
                    Grammar.build(ARRAY_ITEM)
                            .grammars(TOKEN_REFERENCE, GRAMMAR_REFERENCE).match().name("item")
                            .tokens(SPACE, COMMA).match()
                            .token(COMMA).ignore()
                            .token(SPACE).ignore()
            )
            .map(
                    Grammar.build(ARRAY)
                            .token(SPACE).ignore()
                            .token(ARRAY_OPEN).match()
                            .grammar(ARRAY_ITEM).repeat().name("items")
                            .token(ARRAY_CLOSE).match()
                            .token(SPACE).ignore()
            )
            .map(
                    Grammar.build(ROOT_GRAMMAR)
                            .grammar(REPEAT_IGNORE).ignore()
                            .token(IDENTIFYER).match().name("name")
                            .token(SPACE).ignore()
                            .token(COLON).match()
                            .token(SPACE).ignore()
                            .tokens(LINE_END, EOF).match()
                            .grammar(SUB_GRAMMAR).repeat().name("chain")
            )
            .map(
                    Grammar.build(GRAMMAR_FILE)
                            .grammar(FILE_HEAD).match().name("head")
                            .grammar(ROOT_GRAMMAR).repeat().name("grammars")
            );

    public GrammarParser() {
        super(gram, GRAMMAR_FILE);
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
        JPGenerator gen = new JPGenerator("com.niton.parser.specific.grammar.gen", "D:\\Users\\Nils\\Desktop\\Workspaces\\API\\JainParse\\src\\java\\main");
        gen.generate(gram, GrammarTokens.values());
    }

    /**
     * @throws ParsingException
     * @see com.niton.parser.Parser#convert(GrammarResult)
     */
    @Override
    public GrammarFileContent convert(GrammarResult o) throws ParsingException {
        GrammarFile g = new GrammarFile((SuperGrammarResult) o);
        GrammarFileContent result = new GrammarFileContent();
        for (IgnoringTokenDefiner definer : g.getHead().getTokenDefiners()) {
            result.getTokens().put(definer.getContent().getName(), new Token(definer.getContent().getLiteral().getRegex().replaceAll("\\\\'", "'")));
        }
        for (RootGrammar gram : g.getGrammars()) {
            ChainGrammar gr = Grammar.build(gram.getName());
            for (SubGrammar r : gram.getChain()) {
                String name = null;
                String operation = r.getOperation();
                if (r.getAssignment() != null)
                    name = r.getAssignment().getName();

                Grammar chainElement;

                //Item parsing
                if (r.getItem().getType().equals(GRAMMAR_REFERENCE.getName())) {
                    com.niton.parser.specific.grammar.gen.GrammarReference check = new com.niton.parser.specific.grammar.gen.GrammarReference((SuperGrammarResult) r.getItem().getRes());
                    String grammarName = check.getGrammarName();
                    chainElement = Grammar.reference(grammarName);
                } else if (r.getItem().getType().equals(TOKEN_REFERENCE.getName())) {
                    TokenReference toksub = new TokenReference((SuperGrammarResult) r.getItem().getRes());
                    String token = toksub.getTokenName();
                    chainElement = Grammar.tokenReference(token);
                } else if (r.getItem().getType().equals(ARRAY.getName())) {
                    Array arr = new Array((SuperGrammarResult) r.getItem().getRes());
                    if (arr.getItems().size() == 0) {
                        throw new ParsingException("Empty arrays are not allowed (and they don't make any sense)");
                    } else if (arr.getItems().get(0).getItem().getType().equals(GRAMMAR_REFERENCE.getName())) {
                        Grammar[] grms = new Grammar[arr.getItems().size()];
                        for (int i = 0; i < grms.length; i++) {
                            if(arr.getItems().get(i).getItem().getType().equals(TOKEN_REFERENCE.getName()))
                                throw new ParsingException("It is not allowed to mix Grammars and tokens in arrays");
                            grms[i] = Grammar.reference(new com.niton.parser.specific.grammar.gen.GrammarReference((SuperGrammarResult) arr.getItems().get(i).getItem().getRes()).getGrammarName());
                        }
                        chainElement = Grammar.anyOf(grms);
                    }else if (arr.getItems().get(0).getItem().getType().equals(TOKEN_REFERENCE.getName())) {
                        Grammar[] grms = new Grammar[arr.getItems().size()];
                        for (int i = 0; i < grms.length; i++) {
                            if(arr.getItems().get(i).getItem().getType().equals(GRAMMAR_REFERENCE.getName()))
                                throw new ParsingException("It is not allowed to mix Grammars and tokens in arrays");
                            grms[i] = Grammar.tokenReference(new com.niton.parser.specific.grammar.gen.TokenReference((SuperGrammarResult) arr.getItems().get(i).getItem().getRes()).getTokenName());
                        }
                        chainElement = Grammar.anyOf(grms);
                    }else {
                        throw new ParsingException("The item type \"" + arr.getItems().get(0).getItem().getType() + "\" is an unkown type to us!");
                    }
                } else {
                    throw new ParsingException("The item type \"" + r.getItem().getType() + "\" is an unkown type to us!");
                }

                //apply operations
                if (operation.equals(ANY_EXCEPT_SIGN))
                    chainElement = Grammar.anyExcept(chainElement);
                else if (operation.equals(IGNORE_SIGN))
                    chainElement = Grammar.ignore(chainElement);
                else if (operation.equals(OPTIONAL_SIGN))
                    chainElement = Grammar.optional(chainElement);

                //apply repeat
                if (r.getRepeat() != null)
                    chainElement = Grammar.repeat(chainElement);

                gr.grammar(chainElement).match();
                gr.name(name);
            }
            result.getGrammars().map(gr);
        }
        return result;
    }

}

