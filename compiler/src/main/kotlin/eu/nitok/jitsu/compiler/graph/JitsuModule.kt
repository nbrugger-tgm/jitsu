package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.compiler.graph.behaviour.ScopeProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.IdentityHashMap

@Serializable
class JitsuModule internal constructor(
    /**
     * Full qualified name
     */
    val name: String,
    val files: List<JitsuFile>,
    val submodules: List<JitsuModule>,
    private val typeDb:IrStore<TypeDefinition> = IrStore(),
    private val functionDb:IrStore<Function> = IrStore(),
    private val variableDb:IrStore<Variable> = IrStore()
) : Element, ScopeProvider {

    init {
        fun informElements(elem: List<Element>) {
            elem.forEach { element ->
                if(element is ModuleAware) element.setEnclosingModule(this)
                if(element !is JitsuModule) informElements(element.children)
            }
        }
        informElements(children)
    }

    @Transient val messages = CompilerMessages()

    override val children: List<Element> get() = files + submodules
    val allModules: Sequence<JitsuModule> get() = sequenceOf(this) + submodules.flatMap { it.allModules }
    val moduleLookup: Map<String, JitsuModule> by lazy { allModules.associateBy { it.name } }

    @Transient private val allFunctions = files.flatMap { it.functions }.filter { it.name != null }.groupBy { it.name!!.value }
    @Transient private val allTypes = files.flatMap { it.types }.associateBy { it.name.value }

    @Transient
    override val scope: Scope = Scope(
        functions = allFunctions,
        types = allTypes
    )

    @Transient
    val exportScope: Scope = Scope(
        functions = allFunctions,//.filter { it.exported }
        types = allTypes //.filter { it.exported }
    )


    fun getSymbolID(type: TypeDefinition): SymbolID {
        return SymbolID(name, typeDb.getSymbolId(type))
    }
    fun getSymbolID(type: Function): SymbolID {
        return SymbolID(name, functionDb.getSymbolId(type))
    }
    fun getSymbolID(type: Variable): SymbolID {
        return SymbolID(name, variableDb.getSymbolId(type))
    }
    fun getType(id: Int) = typeDb[id]
    fun getFunction(id: Int) = functionDb[id]
    fun getVariable(id: Int) = variableDb[id]


//    val exportedFunctions = merge(*files.map { file -> file.functions }.toTypedArray()) { a, b -> a + b }
//    val exportedTypes = merge(*files.map { file -> file.types.map { it to file } }.toTypedArray()) { a, b ->
//        b.second.messages.error("Type with name ${b.first.name.value} is already exported by module",b.first.name.location, CompilerMessage.Hint(
//            "First definition", a.first.name.location
//        ))
//        a
//    }.mapValues { it.value.first }
//    val exportedVariables = merge(*files.map { file -> file.variables.map { it to file } }.toTypedArray())  { a, b ->
//        b.second.messages.error("Variable with name ${b.first.name.value} is already exported by module",b.first.name.location, CompilerMessage.Hint(
//            "First definition", a.first.name.location
//        ))
//        a
//    }.mapValues { it.value.first }
}

@Serializable
internal class IrStore<T> {
    @Transient private val funcIds = IdentityHashMap<T, Int>()
    @Transient private var nextFuncId = 0;
    private val db: MutableList<T> = mutableListOf()
    fun getSymbolId(func: T): Int {
        return funcIds.getOrPut(func) {
            db.add(func)
            nextFuncId++
        }
    }
    operator fun get(id: Int): T {
        return db[id]
    }
}