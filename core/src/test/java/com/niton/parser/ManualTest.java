package com.niton.parser;

import com.niton.parser.ast.ReducedNode;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammar.api.GrammarReferenceMap;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

import static com.niton.parser.grammar.api.Grammar.*;
import static com.niton.parser.token.DefaultToken.*;

public class ManualTest {
	public static void main(String[] args) , IOException {
		GrammarReferenceMap ref = new GrammarReferenceMap();
		ref.map(
				   build("Number")
						   .token(NUMBER).add("value")
		   ).map(
				   build("Singed Number")
						   .tokens(PLUS, MINUS).add("sign")
						   .token(NUMBER).add("value")
		   )

		   .map(
				   build("Multiplicative")
						   .tokens(STAR, SLASH).add("operator")
						   .grammar("Non calc expression").add("expression")
		   )
		   .map(
				   build("Additive")
						   .tokens(PLUS, MINUS).add("operator")
						   .grammar("Expression").add("expression")
		   )
		   .map(
				   anyOf(
						   reference("Multiplicative"),
						   reference("Additive")
				   ).setName("Operation")
		   )
		   .map(
				   anyOf(
						   reference("Number"),
						   reference("Singed Number"),
						   reference("Enclosed Expression")
				   ).setName("Non calc expression")
		   )
		   .map(
				   build("Expression")
						   .grammar("Non calc expression").add("startExpression")
						   .grammar("Operation").repeat().add("operations")
		   ).map(
				   build("Enclosed Expression")
						   .token(BRACKET_OPEN).add()
						   .grammar("Expression").add("expression")
						   .token(BRACKET_CLOSED).add()
		   );
		DefaultParser parser  = new DefaultParser(ref, "Expression");
		var           result  = parser.parse("(10*10)/5+24/-12-2");
		var           reduced = result.reduce("expression").orElseThrow();
		serveAstHtml(reduced);
		System.out.println(reduced.format());
		System.out.println(result.joinTokens() + " = " + calculateExpression(reduced));
	}

	private static void serveAstHtml(ReducedNode reduced) throws IOException {
		var html   = getAstHtml(reduced);
		var server = HttpServer.create();
		server.createContext("/").setHandler(exchange -> {
			exchange.sendResponseHeaders(200, html.length());
			exchange.getResponseBody().write(html.getBytes());
			exchange.close();
		});
		server.setExecutor(null);
		server.bind(new InetSocketAddress("0.0.0.0", 80), 1);
		server.start();
	}

	private static String getAstHtml(ReducedNode reduced) {
		return "<!DOCTYPE html>\n" +
				"<html>\n" +
				"<head>\n" +
				"<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
				"<style>\n" +
				"ul, #myUL {\n" +
				"  list-style-type: none;\n" +
				"}\n" +
				"\n" +
				"#myUL {\n" +
				"  margin: 0;\n" +
				"  padding: 0;\n" +
				"}\n" +
				"\n" +
				".node {\n" +
				"  cursor: pointer;\n" +
				"  -webkit-user-select: none; /* Safari 3.1+ */\n" +
				"  -moz-user-select: none; /* Firefox 2+ */\n" +
				"  -ms-user-select: none; /* IE 10+ */\n" +
				"  user-select: none;\n" +
				"}\n" +
				"\n" +
				".node::before {\n" +
				"  content: \"\\25B6\";\n" +
				"  color: black;\n" +
				"  display: inline-block;\n" +
				"  margin-right: 6px;\n" +
				"}\n" +
				"\n" +
				".caret-down::before {\n" +
				"  -ms-transform: rotate(90deg); /* IE 9 */\n" +
				"  -webkit-transform: rotate(90deg); /* Safari */'\n" +
				"  transform: rotate(90deg);  \n" +
				"}\n" +
				"\n" +
				".sub-nodes {\n" +
				"  display: none;\n" +
				"}\n" +
				"\n" +
				".active {\n" +
				"  display: block;\n" +
				"}\n" +
				"</style>\n" +
				"</head>\n" +
				"<body>\n" +
				"\n" +
				"<h2>Tree View</h2>\n" +
				"<p>A tree view represents a hierarchical view of information, where each item can have a number of subitems.</p>\n" +
				"<p>Click on the arrow(s) to open or close the tree branches.</p>\n" +
				"\n" +
				"<ul id=\"myUL\">\n" +
				reduced.formatHtml() +
				"</ul>\n" +
				"\n" +
				"<script>\n" +
				"var toggler = document.getElementsByClassName(\"node\");\n" +
				"var i;\n" +
				"\n" +
				"for (i = 0; i < toggler.length; i++) {\n" +
				"  toggler[i].addEventListener(\"click\", function() {\n" +
				"    this.parentElement.querySelector(\".sub-nodes\").classList.toggle(\"active\");\n" +
				"    this.classList.toggle(\"caret-down\");\n" +
				"  });\n" +
				"}\n" +
				"</script>\n" +
				"\n" +
				"</body>\n" +
				"</html>\n";
	}

	private static int calculateExpression(ReducedNode result)  {
		var start      = result.getSubNode("startExpression").orElseThrow();
		var startValue = calculateNoCalcExpression(start);
		var operations = result.getSubNode("operations").orElseThrow();
		for (ReducedNode op : operations.getChildren()) {
			startValue = calculateOperator(startValue, op);
		}
		return startValue;
	}

	private static int calculateOperator(int startValue, ReducedNode op)  {
		var type = op.getSubNode("type").orElseThrow().getValue();
		op = op.getSubNode("value").orElseThrow();
		var operator   = op.getSubNode("operator").map(ReducedNode::getValue).orElseThrow();
		var expression = op.getSubNode("expression").orElseThrow();
		int expressionVal;
		switch (type) {
			case "Additive":
				expressionVal = calculateExpression(expression);
				break;
			case "Multiplicative":
				expressionVal = calculateNoCalcExpression(expression);
				break;
			default:
				throw new ParsingException("No operation type " + type + " known","",0,0,0);
		}
		startValue = applyOperation(startValue, operator, expressionVal);
		return startValue;
	}

	private static int applyOperation(int startValue, String operator, int expressionVal)
			 {
		switch (operator) {
			case "+":
				startValue += expressionVal;
				break;
			case "-":
				startValue -= expressionVal;
				break;
			case "*":
				startValue *= expressionVal;
				break;
			case "/":
				startValue /= expressionVal;
				break;
			default:
				throw new ParsingException("No operator " + operator + " known","",0,0,0);
		}
		return startValue;
	}

	private static int calculateNoCalcExpression(ReducedNode result)  {
		var type = result.getSubNode("type").orElseThrow().getValue();
		switch (type) {
			case "Number":
				return calculateNumber(result.getSubNode("value").orElseThrow());
			case "Singed Number":
				return calculateSignedNumber(result.getSubNode("value").orElseThrow());
			case "Enclosed Expression":
				return calculateEnclosedExpression(result.getSubNode("value").orElseThrow());
			default:
				throw new RuntimeException("Unknown expression type: " + type);
		}
	}

	private static int calculateEnclosedExpression(ReducedNode node)  {
		return calculateExpression(node.getSubNode("expression").orElseThrow());
	}

	private static int calculateSignedNumber(ReducedNode node) {
		var value = calculateNumber(node);
		return node.getSubNode("sign").orElseThrow().getValue().equals("+") ? value : -value;
	}

	private static int calculateNumber(ReducedNode value) {
		return Integer.parseInt(value.getSubNode("value").orElseThrow().getValue());
	}
}
