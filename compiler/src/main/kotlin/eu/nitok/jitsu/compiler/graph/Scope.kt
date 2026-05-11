package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.common.CompilerMessage
import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.compiler.analysis.FunctionSignatureMatch
import eu.nitok.jitsu.compiler.analysis.matchFunctionSignatures

/**
 * Content of a file or everything that is between { and }
 */
class Scope(
    private val types: Map<String, TypeDefinition> = mapOf(),
    private val functions: Map<String, List<Function>> = mapOf(),
    private val variables: Map<String, Variable> = mapOf(),
    private val imports: List<Import> = listOf(),
) {
    var parent: Scope? = null
    val allFunctions: Map<String, List<Function>>
        get() = merge(
            functions,
            parent?.allFunctions ?: emptyMap(),
            *imports.mapNotNull { it.target?.exportScope?.functions }.toTypedArray()
        ) { a, b -> a + b }

    val allTypes: Map<String, TypeDefinition>
        get() = merge(
            types,
            parent?.allTypes ?: emptyMap(),
            *imports.mapNotNull { it.target?.exportScope?.types }.toTypedArray()
        ) { a, _ -> a } //shadowing

    val allVariables: Map<String, Variable>
        get() = merge(
            variables,
            parent?.allVariables ?: emptyMap(),
            *imports.mapNotNull { it.target?.exportScope?.variables }.toTypedArray()
        ) { a, _ -> a }//shadowing

    fun resolveType(reference: Located<String>, messages: CompilerMessages): TypeDefinition {
        return types[reference.value] ?: parent?.resolveType(reference, messages) ?: run {
            messages.error("Type with name '${reference.value}' does not exist", reference.location)
            TypeDefinition.ParameterizedType.Alias(reference, listOf(), Type.Undefined)
        }
    }

    fun resolveFunction(
        name: Located<String>,
        parameterTypes: Array<Located<Type>>,
        messages: CompilerMessages
    ): Function? {
        val matchingFunctions = allFunctions[name.value] ?: run {
            messages.error("No function named '${name.value}'", name.location)
            return null
        }
        return when (val match = matchFunctionSignatures(matchingFunctions, parameterTypes)) {
            is FunctionSignatureMatch.NoMatch -> {
                messages.error(
                    "No function named '${name.value}' with matching signature",
                    name.location,
                    *matchingFunctions.map {
                        CompilerMessage.Hint("Signature: ${it.signature}", it.name!!.location)
                    }.toTypedArray()
                )
                null
            }

            is FunctionSignatureMatch.Suggestion -> {
                match.missing.forEach {
                    messages.error(
                        "Missing parameter '${it.name.value}'",
                        name.location,
                        CompilerMessage.Hint("Parameter '${it.name.value}' defined here", it.name.location)
                    )
                }
                match.typeError.forEach {
                    val fullMesageChain = it.reason.fullMessageChain()
                    messages.error(
                        "Type mismatch for parameter '${it.paramDefinition.value}': '${it.expected}', '${fullMesageChain.first}'",
                        it.parameterValueLocation,
                        CompilerMessage.Hint(
                            "Parameter '${it.paramDefinition.value}' defined here",
                            it.paramDefinition.location
                        ),
                        *fullMesageChain.second.toTypedArray()
                    )
                }
                if (match.overflow > 0) {
                    messages.error(
                        "Too many parameters (expected: ${match.function.parameters.filter { it.initialValue == null }.size}, got: ${parameterTypes.size})",
                        name.location,
                        CompilerMessage.Hint("Function defined here", match.function.name!!.location)
                    )
                }
                match.function
            }

            is FunctionSignatureMatch.Match -> match.function
        }
    }

    fun resolveVariable(located: Located<String>, messages: CompilerMessages): Variable? {
        val self = variables[located.value]
        if (self != null) return self
        if (parent == null) {
            messages.error("No variable named '${located.value}'", located.location)
            return null
        }
        return parent!!.resolveVariable(located, messages)
    }

    fun restoreType(id: Located<SymbolID>, messages: CompilerMessages): TypeDefinition? {
       return restore(id, messages, JitsuModule::getType)
    }
    fun restoreFunction(id: Located<SymbolID>, messages: CompilerMessages): TypeDefinition? {
        return restore(id, messages, JitsuModule::getType)
    }
    fun restoreVariable(id: Located<SymbolID>, messages: CompilerMessages): TypeDefinition? {
        return restore(id, messages, JitsuModule::getType)
    }

    fun <T> restore(id: Located<SymbolID>, messages: CompilerMessages, getElement: JitsuModule.(Int)->T?): T? {
        if(id.value.module == null) throw IllegalArgumentException("restore() requires module name to be set")
        val import = resolveModule(id.value.module!!)
        if(import == null) messages.error("Unable to restore symbol ${id.value}: Module ${id.value.module} not found", id)
        return import?.target?.getElement(id.value.index)
    }

    private fun resolveModule(module: String): Import? = imports.find { it.name.value == module }?: parent?.resolveModule(module)
}

fun <T> ensureSingleName(
    it: Map.Entry<String, List<T>>,
    messages: CompilerMessages,
    subjects: String,
    namer: T.() -> Located<String>
): T {
    val first = it.component2().first()
    it.component2().drop(1).forEach {
        messages.error(
            "$subjects with name '${it.namer().value}' already exists",
            it.namer().location,
            CompilerMessage.Hint("Already defined here", first.namer().location)
        )
    }
    return first
}