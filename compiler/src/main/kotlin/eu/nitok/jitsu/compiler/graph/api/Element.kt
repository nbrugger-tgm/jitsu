package eu.nitok.jitsu.compiler.graph.api

import eu.nitok.jitsu.common.Walkable

/**
 * Element in the language IR graph
 */
interface Element: Walkable<Element>