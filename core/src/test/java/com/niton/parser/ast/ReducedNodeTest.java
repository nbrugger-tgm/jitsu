package com.niton.parser.ast;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ReducedNodeTest {
	@Nested
	class LeafTest {
		@Test
		void leafSpec() {
			var leaf = ReducedNode.leaf("test", "value");
			assertThat(leaf.getName()).isEqualTo("test");
			assertThat(leaf.getValue()).isEqualTo("value");
			assertThat(leaf.isLeaf()).isTrue();
		}

		@Test
		void noChildren() {
			var leaf = ReducedNode.leaf("test", "value");
			assertThatCode(leaf::getChildren).isInstanceOf(UnsupportedOperationException.class);
			assertThatCode(leaf::getChildNames).isInstanceOf(UnsupportedOperationException.class);
			assertThatCode(() -> leaf.getSubNode("test")).isInstanceOf(UnsupportedOperationException.class);
		}
	}

	@Nested
	class NodeTest {
		@Test
		void nodeSpec() {
			var leaf = ReducedNode.leaf("test", "value");
			var node = ReducedNode.node("test", List.of(leaf));
			assertThat(node.getName()).isEqualTo("test");
			assertThat(node.getChildren()).containsExactly(leaf);
			assertThat(node.isLeaf()).isFalse();
		}

		@Test
		void noValue() {
			var leaf = ReducedNode.leaf("test", "value");
			var node = ReducedNode.node("test", List.of(leaf));
			assertThatCode(node::getValue).isInstanceOf(UnsupportedOperationException.class);
		}

		@Test
		void childNames() {
			var leaf  = ReducedNode.leaf("test", "value");
			var leaf1 = ReducedNode.leaf("test1", "value");
			var leaf2 = ReducedNode.leaf("test2", "value");
			var node  = ReducedNode.node("test", List.of(leaf, leaf1, leaf2));
			assertThat(node.getChildNames()).containsExactly("test", "test1", "test2");
		}

		@Test
		void getChild() {
			var leaf  = ReducedNode.leaf("test", "value");
			var leaf1 = ReducedNode.leaf("test1", "value2");
			var leaf2 = ReducedNode.leaf("test2", "value3");
			var node  = ReducedNode.node("test", List.of(leaf, leaf1, leaf2));
			assertThat(node.getSubNode("test1")).isPresent().contains(leaf1);
		}

		@Test
		void getNoChild() {
			var leaf  = ReducedNode.leaf("test", "value");
			var leaf1 = ReducedNode.leaf("test1", "value2");
			var leaf2 = ReducedNode.leaf("test2", "value3");
			var node  = ReducedNode.node("test", List.of(leaf, leaf1, leaf2));
			assertThat(node.getSubNode("test3")).isNotPresent();
		}

		@Test
		void parenting() {
			var leaf  = ReducedNode.leaf("test", "value");
			var leaf1 = ReducedNode.leaf("test1", "value2");
			var leaf2 = ReducedNode.leaf("test2", "value3");
			var node  = ReducedNode.node("test", List.of(leaf, leaf1, leaf2));
			assertThat(leaf1.getParent()).isSameAs(node);
		}
	}

	@Test
	void html() {
		var leaf11 = ReducedNode.leaf("test1", "value1");
		var leaf12 = ReducedNode.leaf("test2", "value2");
		var leaf21 = ReducedNode.leaf("test1", "value1");
		var leaf22 = ReducedNode.leaf("test2", "value2");
		var snode1 = ReducedNode.node("sub1", List.of(leaf11, leaf12));
		var snode2 = ReducedNode.node("sub2", List.of(leaf21, leaf22));
		var node   = ReducedNode.node("root", List.of(snode1, snode2));
		var expetedHtml = "<li>\n" +
				"\t<span class=\"node\">root</span>\n" +
				"\t<ul class=\"sub-nodes\">\n" +
				"\t\t<li>\n" +
				"\t\t\t<span class=\"node\">sub1</span>\n" +
				"\t\t\t<ul class=\"sub-nodes\">\n" +
				"\t\t\t\t<li>test1: value1</li>\n" +
				"\t\t\t\t<li>test2: value2</li>\n" +
				"\t\t\t</ul>\n" +
				"\t\t</li>\n" +
				"\t\t<li>\n" +
				"\t\t\t<span class=\"node\">sub2</span>\n" +
				"\t\t\t<ul class=\"sub-nodes\">\n" +
				"\t\t\t\t<li>test1: value1</li>\n" +
				"\t\t\t\t<li>test2: value2</li>\n" +
				"\t\t\t</ul>\n" +
				"\t\t</li>\n" +
				"\t</ul>\n" +
				"</li>";
		assertThat(node.formatHtml()).isEqualTo(expetedHtml);
	}

	@Test
	void format() {
		var leaf11 = ReducedNode.leaf("test1", "value1");
		var leaf12 = ReducedNode.leaf("test2", "value2");
		var leaf21 = ReducedNode.leaf("test1", "value1");
		var leaf22 = ReducedNode.leaf("test2", "value2");
		var snode1 = ReducedNode.node("sub1", List.of(leaf11, leaf12));
		var snode2 = ReducedNode.node("sub2", List.of(leaf21, leaf22));
		var node   = ReducedNode.node("root", List.of(snode1, snode2));
		var expetedHtml = "root\n" +
				"|-sub1\n" +
				"| |-test1: value1\n" +
				"| `-test2: value2\n" +
				"`-sub2\n" +
				"  |-test1: value1\n" +
				"  `-test2: value2\n";
		assertThat(node.format()).isEqualTo(expetedHtml);
	}
}