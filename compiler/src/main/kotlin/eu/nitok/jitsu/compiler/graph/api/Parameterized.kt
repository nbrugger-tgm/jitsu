package eu.nitok.jitsu.compiler.graph.api

import eu.nitok.jitsu.compiler.graph.api.TypeDefinition.DirectTypeDefinition.TypeParameter

interface Parameterized {
    val typeParameters: List<TypeParameter>
}