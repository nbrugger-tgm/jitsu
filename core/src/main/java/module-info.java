module jainparse.core {
	requires static lombok;
	requires static org.jetbrains.annotations;

	exports com.niton.parser.token;
	exports com.niton.parser.exceptions;
	exports com.niton.parser.ast;
	exports com.niton.parser.grammar;
	exports com.niton.parser.grammar.api;
	exports com.niton.parser.grammar.matchers;
	exports com.niton.parser.grammar.types;
}