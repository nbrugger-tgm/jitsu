package com.niton.parser.ast;

import lombok.Data;

import java.util.List;
import java.util.Optional;

@Data
public class ReducedNode {
	private final List<ReducedNode> children;
	private final String            value;
	private final boolean           isLeaf;
	private final String            name;
	private       ReducedNode       parent;

	private ReducedNode(
			List<ReducedNode> children,
			String value,
			boolean isLeaf,
			String name
	) {
		if (children != null) {
			children.forEach(n -> n.setParent(this));
		}
		this.children = children;
		this.value    = value;
		this.isLeaf   = isLeaf;
		this.name     = name;
	}

	private void setParent(ReducedNode parent) {
		this.parent = parent;
	}

	public static ReducedNode leaf(String name, String value) {
		return new ReducedNode(null, value, true, name);
	}

	public static ReducedNode node(String name, List<ReducedNode> children) {
		return new ReducedNode(children, null, false, name);
	}

	public Optional<ReducedNode> getSubNode(String name) {
		return children.stream().filter(n -> n.name.equals(name)).findFirst();
	}

	public String getValue() {
		if (!isLeaf()) {
			throw new UnsupportedOperationException("Only leafs can have values");
		}
		return value;
	}

	public List<ReducedNode> getChildren() {
		if (isLeaf()) {
			throw new UnsupportedOperationException("Only nodes can have children");
		}
		return children;
	}

	public List<String> getChildNames() {
		return children.stream().map(n -> n.name).collect(java.util.stream.Collectors.toList());
	}

	public String format() {
		if (isLeaf()) {
			return String.format("%s: %s", name, value);
		}
		StringBuilder sb = new StringBuilder();
		sb.append(name).append("\n");
		formatChildren(sb);
		return sb.toString();
	}

	private void formatChildren(StringBuilder sb) {
		for (int i = 0; i < children.size(); i++) {
			var child    = children.get(i);
			var childStr = child.format();
			formatChild(sb, i, childStr);
		}
	}

	private void formatChild(StringBuilder sb, int childIndex, String childBlock) {
		String[] lines = childBlock.split("\n");
		for (int j = 0; j < lines.length; j++) {
			if (j == 0) {
				if (childIndex + 1 < children.size()) {
					sb.append("|-");
				} else {
					sb.append("`-");
				}
			} else if (childIndex + 1 < children.size()) {
				sb.append("| ");
			} else {
				sb.append("  ");
			}

			sb.append(lines[j]);
			//if (childIndex + 1 < children.size())
			sb.append("\n");
		}
	}

	public String formatHtml() {
		StringBuilder sb = new StringBuilder();
		if (isLeaf()) {
			sb.append("<li>")
			  .append(name)
			  .append(": ")
			  .append(value)
			  .append("</li>");
		} else {
			sb.append("<li>\n")
			  .append("\t<span class=\"node\">")
			  .append(name)
			  .append("</span>\n")
			  .append("\t<ul class=\"sub-nodes\">");
			for (var child : children) {
				sb.append(child.formatHtml().replace("\n", "\n\t"));
			}
			sb.append("\n\t</ul>")
			  .append("\n</li>");
		}
		return sb.toString();
	}
}
