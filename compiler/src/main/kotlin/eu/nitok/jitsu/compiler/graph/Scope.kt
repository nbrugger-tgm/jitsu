package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.ast.IdentifierNode
import eu.nitok.jitsu.compiler.ast.Located
import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage
import eu.nitok.jitsu.compiler.model.Walkable
import eu.nitok.jitsu.compiler.parser.Range
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Content of a file or everything that is between { and }
 */
@Serializable
class Scope constructor(
    private val contants: MutableList<Constant<@Contextual Any>> = mutableListOf(),
    private val types: MutableMap<String, TypeDefinition> = mutableMapOf(),
    private val functions: MutableList<Function> = mutableListOf(),
    private val variables: MutableList<Variable> = mutableListOf(),
    val errors: MutableList<CompilerMessage> = mutableListOf(),
    val warnings: MutableList<CompilerMessage> = mutableListOf()
) : Walkable<Scope>, Accessor {
    init {
        functions.forEach { it.scope.parent = this }
    }
    fun getFunctions(): List<Function> = functions

    constructor(parent: Scope) : this() {
        this.parent = parent;
    }

    override val children: List<Scope> get() = functions.map { it.scope }

    @Transient
    var parent: Scope? = null

    fun register(type: TypeDefinition) {
        val existing = types[type.name.value];
        if (existing != null) {
            error(
                "Type with name '${type.name.value}' already exists",
                type.name.location,
                listOf(CompilerMessage.Hint("Already defined here", existing.name.location))
            )
            return
        }
        types[type.name.value] = type
    }

    fun register(func: Function) {
        val existing = if (func.name != null) functions.find { func.name.value == it.name?.value } else null
        if (existing != null) {
            error(
                "Function with name '${func.name?.value}' already exists : {}",
                func.name!!.location,
                listOf(CompilerMessage.Hint("Already defined here", existing.name!!.location))
            )
            return
        }
        functions.add(func);
        func.scope.parent = this;
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

    fun resolveType(reference: Located<String>): TypeDefinition {
        return types[reference.value] ?: run {
            error("Type with name '${reference.value}' does not exist", reference.location)
            TypeDefinition.Alias(reference, listOf(), lazy { Type.Undefined })
        }
    }

    fun resolveFunction(s: String): Function? {
        return functions.find { it.name?.value?.equals(s) ?: false }
    }

    fun resolveVariable(located: Located<String>): Variable? {
        return variables.find { it.name.value == located.value } ?: run {
            error("No variable named '${located.value}'", located.location)
            null
        }
    }

    fun register(variable: Variable) {
        val existing = variables.find { variable.name.value == it.name.value }
        if (existing != null) {
            error(
                "Variable with name '${variable.name.value}' already exists",
                variable.name.location,
                listOf(CompilerMessage.Hint("Already defined here", existing.name.location))
            )
            return
        }
        variables.add(variable)
    }

    override val accessFromSelf: List<Access<*>> get() = functions.flatMap { it.accessFromSelf }
    override val scope: Scope get() = this
}