package com.niton.parser.generator;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AstWalkerGenerator {
	private boolean useInheritance = true;

	public static void main(String[] args) {
		if(args.length != 1)
			throw new IllegalArgumentException("Usage: java -jar AstWalkerGenerator.jar <grammar class>")
					;
	}
}
