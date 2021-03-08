package com.niton.parser.example;

import com.niton.JPGenerator;
import com.niton.ResultDisplay;
import com.niton.parser.*;
import com.niton.parser.example.generated.*;
import com.niton.parser.example.generated.Number;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.result.AnyGrammarResult;
import com.niton.parser.result.SuperGrammarResult;
import com.niton.parser.token.DefaultToken;

import java.io.IOException;

import static com.niton.parser.token.DefaultToken.*;

/**
 * This is the Recursion Class
 *
 * @author Nils Brugger
 * @version 2019-06-07
 */
public class Recursion {

	/**
	 * <b>Description :</b><br>
	 *
	 * @param args
	 * @throws IOException
	 * @throws ParsingException
	 * @author Nils Brugger
	 * @version 2019-06-07
	 */
	public static void main(String... args) throws IOException, ParsingException {
		String s = "1*2*3+4+5+6+7*(+8)/(9*(-10)+9+8+7)*6+5+4+3/2*1+2+3*(4+5+6)+7+8";
		mappingApproach(s);
	}

	private static void mappingApproach(String s) throws ParsingException, IOException {
		System.out.println("Map");
		GrammarReferenceMap ref = new GrammarReferenceMap()
				.deepMap(
						Grammar.build("Number")
						       .token(NUMBER).add("value")
				)
				.deepMap(
						Grammar.build("SignedNumber")
						       .token(BRACKET_OPEN).add()
						       .tokens(PLUS, MINUS).add("sign")
						       .grammar("Number").add("number")
						       .token(BRACKET_CLOSED).add()
				)
				.deepMap(
						Grammar.build("enclosed_expression")
						       .token(BRACKET_OPEN).add()
						       .grammar("expression").add("content")
						       .token(BRACKET_CLOSED).add()
				)
				.deepMap(
						Grammar.build("numeric_expression")
						       .grammars("Number", "SignedNumber").add("content")
				)
				.deepMap(
						Grammar.build("factor_Operand")
						       .tokens(STAR, SLASH).add("operator")
				)
				.deepMap(
						Grammar.build("additional_Operand")
						       .tokens(PLUS, MINUS).add("operator")
				)
				.deepMap(
		            Grammar.build("multiplication")
					         .grammars("division","enclosed_expression","Number","SignedNumber").add("factor1")
				             .token(STAR).add("operand")
				           .grammars("multiplication","division","enclosed_expression","Number","SignedNumber").add("factor2")
				)
				.deepMap(
						Grammar.build("division")
						       .grammars("enclosed_expression","Number","SignedNumber").add("factor1")
						       .token(SLASH).add("operand")
						       .grammars("division","enclosed_expression","Number","SignedNumber").add("factor2")
				)
				.deepMap(
						Grammar.build("addition")
						.grammar("additional_Operand").add("operand")
						.grammars("multiplication","division","enclosed_expression","Number","SignedNumber").add("summand")
				)
				.deepMap(
						Grammar.build("expression")
						       .grammars("multiplication","division","enclosed_expression","Number","SignedNumber").add("start")
								.grammar("addition").repeat().add("operands")

				);
		DefaultParser p   = new DefaultParser(ref, "expression");
		GrammarResult res = p.parse(s);
		System.out.println("Result Coverage : " + res.joinTokens());
		System.out.println(res);
		JPGenerator gen = new JPGenerator("com.niton.parser.example.generated",
		                                  "D:\\Users\\Nils\\Desktop\\Workspaces\\libs\\JainParse\\src\\test\\java");
		gen.generate(ref, DefaultToken.values());
        Expression exp = new Expression((SuperGrammarResult) res);
        System.out.println("");
        System.out.println(s+"="+caclulate(exp));
        ResultDisplay display = new ResultDisplay((SuperGrammarResult) res, ref);
        display.display();
	}
	public static double resolveAnyExpression(AnyGrammarResult val){
		switch (val.getType()) {
			case "enclosed_expression":
				EnclosedExpression encl = new EnclosedExpression((SuperGrammarResult) val.getRes());
				return caclulate(encl.getContent());
			case "Number":
				return Integer.parseInt(new Number((SuperGrammarResult) val.getRes()).getValue());
			case "SignedNumber":
				Signednumber nbr = new Signednumber((SuperGrammarResult) val.getRes());
				return Integer.parseInt(nbr.getSign()+nbr.getNumber().toString());
			case "multiplication":
				return calculateMultiplication(new Multiplication((SuperGrammarResult) val.getRes()));
			case "division" :
				return calculateDivision(new Division((SuperGrammarResult) val.getRes()));
		}
		throw new RuntimeException("FUCK "+val);
	}

	private static double calculateDivision(Division division) {
		return resolveAnyExpression(division.getFactor1())/resolveAnyExpression(division.getFactor2());
	}

	private static double caclulate(Expression exp) {
		double start = resolveAnyExpression(exp.getStart());
		for (Addition operand : exp.getOperands()) {
			double val = resolveAnyExpression(operand.getSummand());
			if(operand.getOperand().getOperator().equals("-"))
				start -= val;
			else
				start += val;
		}
		System.out.println(exp+"="+start);
		return start;
	}

	private static double calculateMultiplication(Multiplication multiplication) {
		double one = resolveAnyExpression(multiplication.getFactor1());
		double two = resolveAnyExpression(multiplication.getFactor2());
		double res = one * two;
		System.out.println(multiplication+"="+res);
		return res;
	}

//	private static void display(Expression exp) {
//
//	}
}

