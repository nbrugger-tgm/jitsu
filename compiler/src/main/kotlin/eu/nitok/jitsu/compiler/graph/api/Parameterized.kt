package eu.nitok.jitsu.compiler.graph.api

import eu.nitok.jitsu.compiler.graph.api.TypeDefinition.DirectTypeDefinition.TypeParameter

/**
 * Object that may have [typeParameters]. If it does it can be [specialized][Specialized] by [parameterized accesses][AccessWithParameters]
 */
interface Parameterized {
    val typeParameters: List<TypeParameter>
}