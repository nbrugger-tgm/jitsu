package com.niton.parser.specific.grammar.gen;

import com.niton.parser.GrammarObject;
import com.niton.parser.SubGrammerObject;
import com.niton.parser.TokenGrammarObject;
import java.lang.String;

public class MatchOperation {
	private SubGrammerObject obj;

	public MatchOperation(SubGrammerObject obj) {
		this.obj = obj;
	}

	public String getAnyExcept() {
		if(obj.getObject("anyExcept") == null) {
			return null;
		}
		return ((TokenGrammarObject)obj.getObject("anyExcept")).joinTokens();
	}

	public String getOptional() {
		if(obj.getObject("optional") == null) {
			return null;
		}
		return ((TokenGrammarObject)obj.getObject("optional")).joinTokens();
	}

	public String getIgnore() {
		if(obj.getObject("ignore") == null) {
			return null;
		}
		return ((TokenGrammarObject)obj.getObject("ignore")).joinTokens();
	}

	public GrammarObject getCheck() {
		return (GrammarObject) obj.getObject("check");
	}

	public String getRepeat() {
		if(obj.getObject("repeat") == null) {
			return null;
		}
		return ((TokenGrammarObject)obj.getObject("repeat")).joinTokens();
	}

	public NameAssignment getAssignment() {
		return new NameAssignment((SubGrammerObject)obj.getObject("assignment"));
	}
}
