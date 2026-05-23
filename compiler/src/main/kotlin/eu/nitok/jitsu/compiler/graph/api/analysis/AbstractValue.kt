package eu.nitok.jitsu.compiler.graph.api.analysis

import eu.nitok.jitsu.compiler.graph.api.Type

sealed interface AbstractValue {
    interface NoValue : AbstractValue
    interface Const : AbstractValue {
        val value: String
        val valueType: Type
    }

    interface Unknown : AbstractValue
}