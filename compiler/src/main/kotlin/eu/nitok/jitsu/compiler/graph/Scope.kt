package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.ast.CompilerMessages
import eu.nitok.jitsu.compiler.ast.Located
import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage
import eu.nitok.jitsu.compiler.model.sequence
import eu.nitok.jitsu.compiler.parser.Range
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Content of a file or everything that is between { and }
 */
@Serializable
open class Scope constructor(
    private val contants: List<Constant<@Contextual Any>>,
    private val types: Map<String, TypeDefinition>,
    val functions: Map<String, List<Function>>,
    private val variables: Map<String, Variable>
) : Accessor {
    constructor(
        contants: List<Constant<@Contextual Any>>,
        types: List<TypeDefinition>,
        functions: List<Function>,
        variables: List<Variable>,
        messages: CompilerMessages
    ) : this(
        contants,
        types.groupBy { it.name.value }.mapValues { ensureSingleName(it, messages, "Type") { name } },
        functions.filter {
            val unnamed = it.name?.value == null;
            if (unnamed) {
                messages.error("Function without name", Range(0, 0, 0, 0));
            }
            !unnamed
        }.groupBy { it.name!!.value },
        variables.groupBy { it.name.value }.mapValues { ensureSingleName(it, messages, "Variable") { name } }
    )

    val elements: Sequence<Element> get() = types.values.asSequence() + variables.values.asSequence() + contants.asSequence() + functions.values.flatMap { it.asSequence() }


    @Transient
    var parent: Scope? = null
    val allFunctions: Map<String, List<Function>>
        get() = functions.merge(parent?.allFunctions ?: emptyMap()) { a, b -> a + b }

    val allTypes: Map<String, TypeDefinition>
        get() = types.merge(parent?.allTypes ?: emptyMap()) { a, _ -> a } //shadowing

    val allVariables: Map<String, Variable>
        get() = variables.merge(parent?.allVariables ?: emptyMap()) { a, _ -> a }//shadowing

    fun resolveType(reference: Located<String>, messages: CompilerMessages): TypeDefinition {
        return types[reference.value] ?: parent?.resolveType(reference, messages) ?: run {
            messages.error("Type with name '${reference.value}' does not exist", reference.location)
            TypeDefinition.Alias(reference, listOf(), lazy { Type.Undefined })
        }
    }

    fun resolveFunction(name: Located<String>, messages: CompilerMessages): Function? {
        val matchingFunctions = allFunctions[name.value] ?: run {
            messages.error("No function named '${name.value}'", name.location)
            return null
        }
        if (matchingFunctions.size > 1) {
            messages.error(
                "Multiple functions named '${name.value}'! Function overloading is not yet supported!", name.location,
                *matchingFunctions.map { CompilerMessage.Hint("Defined here", it.name!!.location) }.toTypedArray()
            );
            return null
        }
        return matchingFunctions.first()
    }

    fun resolveVariable(located: Located<String>, messages: CompilerMessages): Variable? {
        val self = variables[located.value]
        if(self != null) return self
        if(parent == null) {
            messages.error("No variable named '${located.value}'", located.location)
            return null
        }
        return parent!!.resolveVariable(located, messages)
    }

    fun resolveAll(messages: CompilerMessages) {
        accessFromSelf.forEach { it.resolve(messages) }
        elements.forEach {
            if (it is Accessor) it.accessFromSelf.forEach { it.resolve(messages) }
        }
    }

    @Transient
    override val accessFromSelf: List<Access<*>> = findAccessesFromSelf(elements.asIterable())
}

private fun <T> ensureSingleName(
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