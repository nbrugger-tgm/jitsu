package com.niton.parser.ast;

import com.niton.parser.token.Tokenizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class ListNode extends AstNode {
	private List<AstNode> list = new LinkedList<>();

	public void clear() {
		list.clear();
	}

	public AstNode get(int index) {
		return list.get(index);
	}

	public void add(AstNode element) {
		list.add(element);
	}

	public AstNode remove(int index) {
		return list.remove(index);
	}

	public List<AstNode> getList() {
		return list;
	}

	public void setList(List<AstNode> list) {
		this.list = list;
	}

	@Override
	public Collection<Tokenizer.AssignedToken> join() {
		if (list.size() == 0) {
			return new ArrayList<>(0);
		}
		return list.stream().map(AstNode::join).reduce((a, b) -> {
			a.addAll(b);
			return a;
		}).get();
	}

	@Override
	public ReducedNode reduce(String name) {
		int i = 0;
		List<ReducedNode> reduced = new ArrayList<>();
		for (AstNode node : list) {
			reduced.add(node.reduce(Integer.toString(i++)));
		}
		return ReducedNode.node(name,reduced);
	}

	@Override
	public String toString() {
		StringBuilder bldr = new StringBuilder();
		bldr.append("[").append(getOriginGrammarName()).append("\n");
		for (AstNode grammarResult : list) {
			bldr.append("\t");
			bldr.append(grammarResult.toString());
		}
		bldr.append("]\n");

		return bldr.toString();
	}


}
