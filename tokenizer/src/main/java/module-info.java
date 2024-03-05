module jainparse.tokenizer {
    exports com.niton.jainparse.token;
    requires static lombok;
	requires static org.jetbrains.annotations;
	requires transitive jainparse.core;
}