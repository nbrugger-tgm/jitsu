package eu.nitok.jitsu.compiler.analysis

import eu.nitok.jitsu.compiler.graph.api.Function
import eu.nitok.jitsu.compiler.graph.api.Type

val Function.isMain: Boolean get() = run { name?.value == "main" && parameters.isMain && returnType?.value.isMain }
private val List<Function.Parameter>.isMain: Boolean get() = isEmpty() || size == 1
private val Type?.isMain: Boolean get() = this is Type.Null || this is Type.UInt || this is Type.Int || this == null