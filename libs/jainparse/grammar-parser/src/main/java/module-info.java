module jainparse.grammarparser {
	requires org.jetbrains.annotations;
    requires transitive jainparse.parser;
    requires static lombok;

    exports com.niton.parsers.grammar;
}