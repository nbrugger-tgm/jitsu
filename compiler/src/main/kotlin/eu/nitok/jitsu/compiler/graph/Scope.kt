package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.ast.IdentifierNode
import eu.nitok.jitsu.compiler.ast.Located
import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage
import eu.nitok.jitsu.compiler.parser.Locatable
import eu.nitok.jitsu.compiler.parser.Range
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Content of a file or everything that is between { and }
 */
@Serializable
class Scope(val parent: Scope?) {
    val contants: MutableList<Constant<@Contextual Any>> = mutableListOf();
    val types: MutableMap<String, ResolvedType.NamedType> = mutableMapOf()
    val functions: MutableList<eu.nitok.jitsu.compiler.graph.Function> = mutableListOf()
    val variable: MutableList<Variable> = mutableListOf()
    val errors: MutableList<CompilerMessage> = mutableListOf()
    val warnings: MutableList<CompilerMessage> = mutableListOf()
    fun register(type: ResolvedType.NamedType) {
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

    fun error(message: CompilerMessage) {
        errors.add(message)
    }

    fun error(message: String, location: Locatable, hints: List<CompilerMessage.Hint> = emptyList()) {
        errors.add(CompilerMessage(message, location, hints))
    }

    fun warning(message: CompilerMessage) {
        warnings.add(message)
    }

    fun warning(message: String, location: Locatable, hints: List<CompilerMessage.Hint> = emptyList()) {
        warnings.add(CompilerMessage(message, location, hints))
    }

    fun resolveType(reference: Located<String>) : ResolvedType.NamedType {
        val type = types[reference.value]
        if (type == null) {
            error("Type with name '$reference' does not exist", reference.location)
        }
        return ResolvedType.NamedType.Alias(reference, lazy { ResolvedType.Undefined })
    }
}