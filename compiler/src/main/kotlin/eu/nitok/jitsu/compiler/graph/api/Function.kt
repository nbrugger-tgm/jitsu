package eu.nitok.jitsu.compiler.graph.api

import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.compiler.graph.api.analysis.FunctionSummary

interface Function : Instruction, Element, Accessible<Function>, Accessor {
    val returnType: Located<Type>?
    val parameters: List<Parameter>
    val body: Body
    val summary: FunctionSummary?
    val signature: Type.FunctionTypeSignature

    sealed interface Body : Element {
        interface Native : Body
        interface Missing : Body
        interface Implementation : Body, CodeBlock
    }

    interface Parameter : Variable, Element {
        /**
         * alias for [declaredType]
         */
        val type: Type
        override val declaredType: Type
    }
}