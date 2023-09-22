package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.ast.Located
import eu.nitok.jitsu.compiler.ast.N
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Content of a file or everything that is between { and }
 */
@Serializable
class Scope(val parent: Scope?) {
    fun register(name: Located<String>, type: ()->N<ResolvedType>) {
        val existing = types[name.first];
        if(existing != null) {
           error("Type with name '${name.first}' already exists : {}" to name.second, existing.value)
            return
        }
        types[name.first] = lazy { type() to name.second }
    }

    fun error(message: Located<String>, vararg elements: Located<Any>) {
        errors.add(Error(message, listOf(*elements)))
    }

    val contants: MutableList<Constant<@Contextual Any>> = mutableListOf();
    val types: MutableMap<String, Lazy<Located<N<ResolvedType>>>> = mutableMapOf()
    val functions: MutableList<Function> = mutableListOf()
    val variable: MutableList<Variable> = mutableListOf()
    val errors: MutableList<Error> = mutableListOf()
    @Serializable
    data class Error(val message: Located<String>, val elements: List<Located<@Contextual Any>>) {}
}