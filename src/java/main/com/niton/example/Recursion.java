package com.niton.example;

import java.io.IOException;

import com.niton.JPGenerator;
import com.niton.example.generated.Calc_expression;
import com.niton.example.generated.Expression;
import com.niton.example.generated.Number;
import com.niton.parser.*;
import com.niton.parser.grammar.Grammar;
import com.niton.parser.grammar.GrammarMatchGrammer;

/**
 * This is the Recursion Class
 * @author Nils Brugger
 * @version 2019-06-07
 */
public class Recursion {

	/**
	 * <b>Description :</b><br>
	 * 
	 * @author Nils Brugger
	 * @version 2019-06-07
	 * @param args
	 * @throws IOException 
	 * @throws ParsingException 
	 */
	public static void main(String[] args) throws IOException, ParsingException {
		GrammarReference ref = new GrammarReferenceMap()
				.map(
						Grammar.build("Number")
								.matchToken(Tokens.NUMBER,"value")
				)
				.map(
						Grammar.build("calc_expression")
								.matchToken(Tokens.BRACKET_OPEN).match("expression","firstExpression").matchAnyToken("calculationType",Tokens.MULTIPLICATOR,Tokens.PLUS,Tokens.MINUS,Tokens.SLASH).match("expression","secondExpression").matchToken(Tokens.BRACKET_CLOSED))
				.map(Grammar.build("expression").matchAny("matched", new String[]{"Number","calc_expression"}));
		String s = "(1+2)";
//		JPGenerator gen = new JPGenerator("com.niton.example.generated", "D:\\Users\\Nils\\Desktop\\Workspaces\\API\\JainParse\\src\\java\\main");
//		gen.generate(ref);
		DefaultParser p = new DefaultParser(ref, "expression");
		GrammarObject res = p.parse(s);
		Expression exp = new Expression((SubGrammerObject) res);
		display(exp);
		System.out.println("");
		System.out.println(caclulate(exp));

	}

	private static int caclulate(Expression exp) {
		if(exp.getMatched().getType().equals("Number")){
			Number nb = new Number(exp.getMatched());
			return Integer.parseInt(nb.getValue());
		}else if (exp.getMatched().getType().equals("calc_expression")){
			Calc_expression subExp = new Calc_expression(exp.getMatched());
			int i1 = caclulate(subExp.getFirstExpression());
			int i2 = caclulate(subExp.getSecondExpression());
			switch (subExp.getCalculationType()){
				case "+":
					return i1+i2;
				case "*":
					return i1*i2;
				case "/":
					return i1/i2;
				case "-":
					return i1-i2;
			}
		}
		return 0;
	}

	private static void display(Expression exp) {
		if(exp.getMatched().getType().equals("Number")){
			Number nb = new Number(exp.getMatched());
			System.out.print(nb.getValue());
		}else if (exp.getMatched().getType().equals("calc_expression")){
			Calc_expression subExp = new Calc_expression(exp.getMatched());
			display(subExp.getFirstExpression());
			System.out.print(subExp.getCalculationType());
			display(subExp.getSecondExpression());
		}
	}
}

