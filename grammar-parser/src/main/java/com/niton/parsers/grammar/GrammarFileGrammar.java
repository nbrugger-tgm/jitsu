package com.niton.parsers.grammar;

import com.niton.jainparse.grammar.api.Grammar;
import com.niton.jainparse.grammar.types.ChainGrammar;

import static com.niton.jainparse.grammar.api.Grammar.*;
import static com.niton.parsers.grammar.GrammarFileGrammar.Property.*;
import static com.niton.parsers.grammar.GrammarFileGrammarName.*;
import static com.niton.parsers.grammar.GrammarFileTokens.*;

public class GrammarFileGrammar {
    private static final Grammar<?, GrammarFileTokens> whitespace = anyOf(
            token(SPACE),
            token(LINE_END)
    ).named("whitespace");
    private static final Grammar<?, GrammarFileTokens> comment = Grammar.<GrammarFileTokens>build(COMMENT)
            .token(SLASH).add()
            .token(SLASH).add()
            .token(LINE_END).anyExcept().add(MESSAGE.id())
            .get();
    private static final Grammar<?, GrammarFileTokens> toIgnore = comment.or(whitespace)
            .repeat()
            .named("ignoreable");
    private static final Grammar<?, GrammarFileTokens> tokenLiteral = Grammar.<GrammarFileTokens>build(TOKEN_LITERAL)
            .token(QUOTE).add()
            .token(QUOTE).anyExcept().add(REGEX.id())
            .token(QUOTE).add()
            .get();
    private static final Grammar<?, GrammarFileTokens> tokenDefiner = Grammar.<GrammarFileTokens>build(TOKEN_DEFINER)
            .token(IDENTIFIER).add(NAME.id())
            .token(SPACE).ignore().add()
            .token(EQ).add()
            .token(SPACE).ignore().add()
            .grammar(tokenLiteral).add(LITERAL.id())
            .tokens(LINE_END, EOF).add()
            .get();
    private static final Grammar<?, GrammarFileTokens> fileHead = Grammar.<GrammarFileTokens>build(FILE_HEAD)
            .grammar(
                    toIgnore.ignore().then(tokenDefiner).named("ignoring_toke_definer").repeat()
            ).add("token_definers")
            .get();
    private static final Grammar<?, GrammarFileTokens> grammarReference = token(IDENTIFIER)
            .named(GRAMMAR_REFERENCE.name());
    private static final Grammar<?, GrammarFileTokens> tokenReference = Grammar.<GrammarFileTokens>build(TOKEN_REFERENCE)
            .token(TOKEN_SIGN).add()
            .token(IDENTIFIER).add(TOKEN_NAME.id())
            .get();
    private static final Grammar<?, GrammarFileTokens> nameAssignment = Grammar.<GrammarFileTokens>build(NAME_ASSIGNMENT)
            .token(SPACE).ignore().add()
            .token(ARROW).add()
            .token(SPACE).ignore().add()
            .token(IDENTIFIER).add("name")
            .get();
    private static final Grammar<?, GrammarFileTokens> arrayItem = Grammar.<GrammarFileTokens>build(ARRAY_ITEM)
            .grammars(tokenReference, grammarReference)
            .add(REFERENCE.id())
            .tokens(SPACE, COMMA).optional().add()
            .token(SPACE).repeat().ignore().add()
            .get();
    private static final Grammar<?, GrammarFileTokens> array = Grammar.<GrammarFileTokens>build(ARRAY)
            .token(SPACE).ignore().add()
            .token(ARRAY_OPEN).add()
            .grammar(arrayItem).repeat().add(ITEMS.id())
            .token(ARRAY_CLOSE).add()
            .token(SPACE).ignore().add()
            .get();
    private static final ChainGrammar<GrammarFileTokens> subGrammar =
            Grammar.<GrammarFileTokens>build(SUB_GRAMMAR)
                    .token(SPACE)
                    .ignore()
                    .add()
                    .tokens(ANY_EXCEPT_SIGN, OPTIONAL_SIGN, IGNORE_SIGN)
                    .optional()
                    .add(OPERATION.id())
                    .grammars(tokenReference, grammarReference, array)
                    .add(ITEM.id())
                    .token(SPACE)
                    .ignore()
                    .add()
                    .token(REPEAT_SIGN)
                    .optional()
                    .add(REPEAT.id())
                    .token(SPACE)
                    .ignore()
                    .add()
                    .grammar(nameAssignment)
                    .optional()
                    .add(ASSIGNMENT.id())
                    .tokens(LINE_END, EOF)
                    .add()
                    .get();
    private static final Grammar<?, GrammarFileTokens> chainGrammar = Grammar.<GrammarFileTokens>build(CHAIN_GRAMMAR)
            .grammar(toIgnore).ignore().add()
            .token(IDENTIFIER).add(NAME.id())
            .token(SPACE).ignore().add()
            .token(COLON).add()
            .token(SPACE).ignore().add()
            .tokens(LINE_END, EOF).add()
            .grammar(subGrammar).repeat().add(CHAIN.id())
            .get();
    public static final Grammar<?, GrammarFileTokens> GRAMMAR_FILE_GRAMMAR = Grammar.<GrammarFileTokens>build(GRAMMAR_FILE)
            .grammar(fileHead).add(HEAD.id())
            .grammar(chainGrammar).repeat().add(GRAMMARS.id())
            .get();
    public enum Property {
        MESSAGE,
        REGEX,
        NAME,
        LITERAL,
        TOKEN_DEFINERS,
        TOKEN_NAME,
        ITEM,
        ITEMS,
        OPERATION,
        REPEAT,
        ASSIGNMENT,
        CHAIN,
        HEAD,
        GRAMMARS,
        REFERENCE;

        public String id() {
            return name().toLowerCase();
        }
    }
}
