package com.niton.parser.example;

import com.niton.JPGenerator;
import com.niton.parser.*;
import com.niton.parser.exceptions.ParsingException;
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
		String s = "1+2+1+1+1+1+1+1+1+1+1+1+1+1+1+1+1+1+1+1+1+1+1+1+1+1+";
		mappingApproach(s);
		//treeApproach(s);
	}

	private static void treeApproach(String s) throws ParsingException {
//        System.out.println("Tree");
//        ChainGrammar grm =
//                Grammar.build("expression")
//                        .grammars(
//                                Grammar.build("Number")
//                                        .token(NUMBER).match().name("value"),
//                                Grammar.build("calc_expression")
//                                        .token(BRACKET_OPEN).match()
//                                        .grammar("expression").match().name("firstExpression")
//                                        .tokens(DefaultToken.STAR, DefaultToken.PLUS, DefaultToken.MINUS, DefaultToken.SLASH).match().name("calculationType")
//                                        .grammar("expression").match().name("secondExpression")
//                                        .token(BRACKET_CLOSED).match()
//                        ).match().name("content");
//        DefaultParser p = new DefaultParser(grm);
//        GrammarResult res = p.parse(s);
//        display(exp);
//        System.out.println("");
//        System.out.println(s+"="+caclulate(exp));
	}

	private static void mappingApproach(String s) throws ParsingException, IOException {
		System.out.println("Map");
		GrammarReference ref = new GrammarReferenceMap()
				.map(
						Grammar.build("Number")
						       .token(NUMBER).add("value")
				)
				.map(
						Grammar.build("SignedNumber")
						       .token(BRACKET_OPEN).add()
						       .tokens(PLUS, MINUS).add("sign")
						       .grammar("Number").add("number")
						       .token(BRACKET_CLOSED).add()
				)
				.map(
						Grammar.build("enclosed_expression")
						       .token(BRACKET_OPEN).add()
						       .grammar("expression").add("content")
						       .token(BRACKET_CLOSED).add()
				)
				.map(
						Grammar.build("numeric_expression")
						       .grammars("Number", "SignedNumber").add("content")
				)
				.map(
						Grammar.build("factor_expression")
						       .grammars("numeric_expression", "expression").add("first_expression")
						       .grammar("factor_Operand").add("operand1")
						       .grammar("factor_Operand").repeat().add("operands")
				)
				.map(
						Grammar.build("factor_Operand")
						       .tokens(STAR, SLASH).add("operator")
						       .grammar("expression").add("second_expression")
				)
				.map(
						Grammar.build("additional_expression")
						       .grammars("numeric_expression", "expression").add("first_expression")
						       .grammar("additional_Operand").add("operand1")
						       .grammar("additional_Operand").repeat().name("operands")
				)
				.map(
						Grammar.build("additional_Operand")
						       .tokens(PLUS, MINUS).add("operator")
						       .grammar("expression").add("second_expression")
				)
				.map(
						Grammar.build("calculation_expression")
						       .grammars("factor_expression", "additional_expression")
						       .add("content")
				)
				.map(
						Grammar.build("expression")
						       .grammars("calculation_expression",
						                 "enclosed_expression",
						                 "Number",
						                 "SignedNumber").add("content")
				);
		DefaultParser p   = new DefaultParser(ref, "expression");
		GrammarResult res = p.parse(s);
		System.out.println("Result Coverage : " + res.joinTokens());
		JPGenerator gen = new JPGenerator("com.niton.parser.example.generated",
		                                  "D:\\Users\\Nils\\Desktop\\Workspaces\\API\\JainParse\\src\\java\\test");
		gen.generate(ref, DefaultToken.values());
//        Expression exp = new Expression((SuperGrammarResult) res);
//        display(exp);
//        System.out.println("");
//        System.out.println(s+"="+caclulate(exp));
//        ResultDisplay display = new ResultDisplay((SuperGrammarResult) res);
//        display.display();
	}

//	private static int caclulate(Expression exp) {
//		if(exp.getContent().getType().equals("Number")){
//			Number nb = new Number((SuperGrammarResult) exp.getContent().getRes());
//			return Integer.parseInt(nb.getValue());
//		}else if (exp.getContent().getType().equals("calc_expression")){
//			CalcExpression subExp = new CalcExpression((SuperGrammarResult) exp.getContent().getRes());
//			int i1 = caclulate(subExp.getFirstExpression());
//			int i2 = caclulate(subExp.getSecondExpression());
//			switch (subExp.getCalculationType()){
//				case "+":
//					return i1+i2;
//				case "*":
//					return i1*i2;
//				case "/":
//					return i1/i2;
//				case "-":
//					return i1-i2;
//			}
//		}
//		return 0;
//	}

//	private static void display(Expression exp) {
//
//	}
}

