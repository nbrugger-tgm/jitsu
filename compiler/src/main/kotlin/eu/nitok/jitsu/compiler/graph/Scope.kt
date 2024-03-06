package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.ast.IdentifierNode
import eu.nitok.jitsu.compiler.ast.Located
import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage
import eu.nitok.jitsu.compiler.parser.Range
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Content of a file or everything that is between { and }
 */
@Serializable
class Scope {
    constructor()
    constructor(parent: Scope) {
        this.parent = parent;
    }
    @Transient var parent: Scope? = null
    private val contants: MutableList<Constant<@Contextual Any>> = mutableListOf();
    private val types: MutableMap<String, TypeDefinition> = mutableMapOf()
    private val functions: MutableList<Function> = mutableListOf()
    private val variable: MutableList<Variable> = mutableListOf()
    val errors: MutableList<CompilerMessage> = mutableListOf()
    val warnings: MutableList<CompilerMessage> = mutableListOf()
    fun register(type: TypeDefinition) {
        val existing = types[type.name.value];
        if (existing != null) {
            error(
                "Type with name '${type.name.value}' already exists : {}",
                type.name.location,
                listOf(CompilerMessage.Hint("Already defined here", existing.name.location))
            )
            return
        }
        types[type.name.value] = type
    }
    
    fun register(func: Function) {
        functions.add(func);
    }

    fun error(message: CompilerMessage) {
        errors.add(message)
    }

    fun error(message: String, location: Range, hints: List<CompilerMessage.Hint> = emptyList()) {
        errors.add(CompilerMessage(message, location, hints))
    }

    fun warning(message: CompilerMessage) {
        warnings.add(message)
    }

    fun warning(message: String, location: Range, hints: List<CompilerMessage.Hint> = emptyList()) {
        warnings.add(CompilerMessage(message, location, hints))
    }

    fun resolveType(reference: IdentifierNode) : TypeDefinition {
        return types[reference.value]?: run {
            error("Type with name '$reference' does not exist", reference.location)
            TypeDefinition.Alias(reference.located, listOf(), lazy { Type.Undefined })
        }
    }
}