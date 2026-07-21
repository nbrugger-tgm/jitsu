package eu.nitok.jitsu.compiler.graph.elements

import eu.nitok.jitsu.common.CompilerMessage
import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.compiler.analysis.FunctionSignatureMatch
import eu.nitok.jitsu.compiler.analysis.matchFunctionSignatures
import eu.nitok.jitsu.compiler.graph.SymbolID
import eu.nitok.jitsu.compiler.graph.api.Type
import eu.nitok.jitsu.compiler.graph.api.TypeDefinition
import eu.nitok.jitsu.compiler.graph.api.Variable
import eu.nitok.jitsu.compiler.graph.elements.types.TypeDefinitionElement
import eu.nitok.jitsu.compiler.merge

/**
 * Content of a file or everything that is between { and }
 */
internal class Scope(
    val typeElements: Map<String, TypeDefinitionElement> = mapOf(),
    override val functions: Map<String, List<FunctionElement>> = mapOf(),
    val variableElements: Map<String, VariableElement> = mapOf(),
    override val attributes: Map<String, AttributeDefinitionElement> = mapOf(),
    override val imports: List<Import> = listOf(),
) : eu.nitok.jitsu.compiler.graph.api.Scope {
    override var parent: Scope? = null

    override val types: Map<String, TypeDefinition> by lazy { typeElements.mapValues { it.value.asTypeDefinition } }
    override val variables: Map<String, Variable> by lazy { variableElements.mapValues { it.value.asVariable } }

    fun resolveType(reference: Located<String>, messages: CompilerMessages): TypeDefinitionElement? {
        return typeElements[reference.value] ?: parent?.resolveType(reference, messages) ?: run {
            messages.error("Type with name '${reference.value}' does not exist", reference.location)
            null
        }
    }

    override val allFunctions: Map<String, List<FunctionElement>>
        get() = merge(
            functions,
            parent?.allFunctions ?: emptyMap(),
            *imports.mapNotNull { it.target?.exportScope?.functions }.toTypedArray()
        ) { a, b -> a + b }

    fun resolveFunction(
        name: Located<String>,
        parameterTypes: Array<Located<Type>>,
        messages: CompilerMessages
    ): FunctionElement? {
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

    fun resolveVariable(located: Located<String>, messages: CompilerMessages): VariableElement? {
        val self = variableElements[located.value]
        if (self != null) return self
        if (parent == null) {
            messages.error("No variable named '${located.value}'", located.location)
            return null
        }
        return parent!!.resolveVariable(located, messages)
    }

    fun restoreType(id: Located<SymbolID>, messages: CompilerMessages): TypeDefinitionElement? {
        return restore(id, messages, JitsuModule::getType)
    }

    fun restoreFunction(id: Located<SymbolID>, messages: CompilerMessages): FunctionElement? {
        return restore(id, messages, JitsuModule::getFunction)
    }

    fun restoreVariable(id: Located<SymbolID>, messages: CompilerMessages): VariableElement? {
        return restore(id, messages, JitsuModule::getVariable)
    }

    fun <T> restore(id: Located<SymbolID>, messages: CompilerMessages, getElement: JitsuModule.(Int) -> T?): T? {
        if (id.value.module == null) throw IllegalArgumentException("restore() requires module name to be set")
        val import = resolveModule(id.value.module!!)
        if (import == null) messages.error(
            "Unable to restore symbol ${id.value}: Module ${id.value.module} not found",
            id
        )
        return import?.target?.getElement(id.value.index)
    }

    private fun resolveModule(module: String): Import? =
        imports.find { it.name.value == module } ?: parent?.resolveModule(module)

    fun resolveAttribute(reference: Located<String>, messages: CompilerMessages): AttributeDefinitionElement? {
        val self = attributes[reference.value]
        if (self != null) return self
        val imports = merge(imports.mapNotNull { it.target?.allAttributes?.mapValues { (k, v) -> v to it } }) { a, b ->
            messages.error(
                "Attribute ${a.first.name} is imported from two modules (${a.first.fullyQualifiedName} and ${b.first.fullyQualifiedName})",
                a.second.location,
                CompilerMessage.Hint("Importing ${b.second.name} here", b.second.location)
            )
            null
        }
        val imported = imports[reference.value]
        if (imported != null) return imported.first
        if (parent == null) {
            messages.error("No attribute named '${reference.value}'", reference.location)
            return null
        }
        return parent!!.resolveAttribute(reference, messages)
    }
}
