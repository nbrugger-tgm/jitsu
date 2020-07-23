package com.niton.parser.specific.grammar;

import com.niton.parser.GrammarName;

enum GrammarGrammarName implements GrammarName {
    WHITESPACE,
    COMMENT,
    TO_IGNORE,
    REPEAT_IGNORE,
    TOKEN_LITERAL,
    TOKEN_DEFINER,
    IGNORING_TOKEN_DEFINER,
    FILE_HEAD,
    GRAMMAR_REFERENCE,
    TOKEN_REFERENCE,
    NAME_ASSIGNMENT,
    SUB_GRAMMAR, ARRAY, ARRAY_ITEM, ROOT_GRAMMAR, GRAMMAR_FILE;

    @Override
    public String getName() {
        return name();
    }
}
