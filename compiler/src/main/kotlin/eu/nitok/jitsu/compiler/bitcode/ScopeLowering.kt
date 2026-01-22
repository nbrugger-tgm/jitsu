package eu.nitok.jitsu.compiler.bitcode

import eu.nitok.jitsu.compiler.graph.Function
import eu.nitok.jitsu.compiler.graph.Scope

class FunctionNameRegistry {
    private val funNameMap = mutableMapOf<Function, String>()
    private val nameCountMap = mutableMapOf<String, Int>()
    private var anonymousFunIndex = 0L;
    fun getUniqueName(function: Function): String {
        if (function in funNameMap) {
            return funNameMap[function]!!
        }
        val generatedName = if(function.name == null) {
            "anonymous_$anonymousFunIndex"
        } else {
            if(nameCountMap.containsKey(function.name.value)) {
                val count = nameCountMap[function.name.value]!!
                nameCountMap[function.name.value] = count + 1
                "${function.name}_$count"
            } else {
                nameCountMap[function.name.value] = 1
                function.name.value
            }
        }
        funNameMap[function] = generatedName
        return generatedName
    }
}