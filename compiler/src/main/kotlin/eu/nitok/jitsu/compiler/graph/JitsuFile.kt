package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.graph.behaviour.ScopeProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.net.URI

@Serializable
class JitsuFile internal constructor(
    private val functionIds: List<SymbolID>,
    private val typeIds: List<SymbolID>,
    private val variableIds: List<SymbolID>,
    val imports: List<Import>,
    val path: String
) : Element, ScopeProvider, ModuleAware {

    internal constructor(
        typeDb: IrStore<TypeDefinition> = IrStore(),
        functionDb: IrStore<Function> = IrStore(),
        variableDb: IrStore<Variable> = IrStore(),
        functions: List<Function>,
        types: List<TypeDefinition>,
        variables: List<Variable>,
        imports: List<Import>,
        path: String
    ) : this(
        functions.map { SymbolID(null, functionDb.getSymbolId(it)) },
        types.map { SymbolID(null, typeDb.getSymbolId(it)) },
        variables.map { SymbolID(null, variableDb.getSymbolId(it)) },
        imports,
        path
    )

    val functions by lazy {
        functionIds.map { id -> module.getFunction(id.index) }
    }
    val variables by lazy {
        variableIds.map { id -> module.getVariable(id.index) }
    }
    val types by lazy {
        typeIds.map { id -> module.getType(id.index) }
    }
    val uri by lazy { URI(path) }
    override val children: List<Element> get() = functions + types + variables + imports
    @Transient
    override val scope: Scope = Scope(imports = imports)
    @Transient
    override lateinit var module: JitsuModule
    override fun setEnclosingModule(parent: JitsuModule) {
        module = parent
    }
}