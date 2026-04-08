package eu.nitok.jitsu.backend.c

import eu.nitok.jitsu.compiler.graph.Function

class FunctionNameRegistry {
    private val functionNames = mutableMapOf<Function,String>()
    private var index = 0
    fun getUniqueName(function: Function): String {
        return functionNames.computeIfAbsent(function) {
            if(functionNames.containsValue(it.name?.value)) (it.name?.value?:"anonyomous") + "_"+index++
            else it.name?.value?:"anonyomous"
        }
    }

}