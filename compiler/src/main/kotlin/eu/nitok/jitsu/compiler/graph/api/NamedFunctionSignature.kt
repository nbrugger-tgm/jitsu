package eu.nitok.jitsu.compiler.graph.api

import eu.nitok.jitsu.common.locating.Located

interface NamedFunctionSignature : Element {
    val name: Located<String>
    val typeSignature: Type.FunctionTypeSignature
}