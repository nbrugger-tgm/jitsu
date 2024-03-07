package eu.nitok.jitsu.compiler.analysis

import eu.nitok.jitsu.compiler.graph.Function
import eu.nitok.jitsu.compiler.graph.Type

val Function.isMain: Boolean get() = name?.value == "main" && parameters.isMain && returnType.isMain
private val List<Function.Parameter>.isMain: Boolean get() = isEmpty() || size == 1
private val Type?.isMain: Boolean get() = this == Type.Null || this is Type.UInt || this is Type.Int || this == null