module jainparse.parser {
    requires static lombok;
    requires static org.jetbrains.annotations;
    requires transitive jainparse.core;
    requires transitive jainparse.tokenizer;

    exports com.niton.jainparse.parser;
    exports com.niton.jainparse.grammar.api;
    exports com.niton.jainparse.grammar.types;
    exports com.niton.jainparse.ast;
}