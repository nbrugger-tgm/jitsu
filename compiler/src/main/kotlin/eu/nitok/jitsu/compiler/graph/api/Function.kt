package eu.nitok.jitsu.compiler.graph.api

import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.compiler.graph.api.analysis.FunctionSummary

interface Function : Instruction, Element, Accessor, HasAttributes, Specialized<FunctionDefinition> {
    val returnType: Located<Type>?
    val parameters: List<Parameter>
    val body: Body
    val summary: FunctionSummary?
    val signature: Signature

    sealed interface Body : Element {
        interface Native : Body
        interface Missing : Body
        interface Implementation : Body, CodeBlock
    }

    interface Signature {
        val returnType: Type?
        val parameters: List<Parameter>

        interface Parameter : Element {
            val name: Located<String>
            val type: Type
            val optional: Boolean
        }
    }

    interface Parameter : Variable, Element {
        /**
         * alias for [declaredType]
         */
        override val type: Type
        override val declaredType: Type
    }
}