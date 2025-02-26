module jainparse.core {
	requires static lombok;
	requires static org.jetbrains.annotations;

	exports com.niton.jainparse.internal to jainparse.parser, jainparse.tokenizer;
	exports com.niton.jainparse.exceptions;
	exports com.niton.jainparse.api;
}