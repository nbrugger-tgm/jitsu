package eu.nitok.jitsu.compiler.graph.elements

import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.compiler.graph.IrStore
import eu.nitok.jitsu.compiler.graph.api.Element
import eu.nitok.jitsu.compiler.graph.behaviour.ModuleAware
import eu.nitok.jitsu.compiler.graph.behaviour.ScopeProvider
import eu.nitok.jitsu.compiler.graph.buildJitsuModule
import eu.nitok.jitsu.compiler.graph.elements.types.TypeDefinitionElement
import eu.nitok.jitsu.compiler.graph.restoreJitsuModule
import eu.nitok.jitsu.parser.ast.JitsuModuleAst
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText
import eu.nitok.jitsu.compiler.graph.api.JitsuModule as JitsuModulePublicApi

@Serializable
internal class JitsuModule internal constructor(
    /**
     * Full qualified name
     */
    override val fullyQualifiedName: String,
    override val files: List<JitsuFile>,
    override val submodules: List<JitsuModule>,
) : ScopeProvider, JitsuModulePublicApi {

    @Transient private val typeDb: IrStore<TypeDefinitionElement> = IrStore()
    @Transient private val functionDb: IrStore<FunctionElement> = IrStore()
    @Transient private val variableDb: IrStore<VariableElement> = IrStore()

    init {
        fun informElements(elem: List<Element>) {
            elem.forEach { element ->
                if (element is ModuleAware) element.setEnclosingModule(this)
                if (element !is JitsuModule) informElements(element.children)
            }
        }
        informElements(children)
    }

    @Transient
    override val messages = CompilerMessages()

    override val children: List<Element> get() = files + submodules

    override val allModules: Sequence<JitsuModule> get() = sequenceOf(this) + submodules.flatMap { it.allModules }
    override val moduleLookup: Map<String, JitsuModule> by lazy { allModules.associateBy { it.fullyQualifiedName } }

    @Transient
    override val allFunctions = files.flatMap { it.functions }.filter { it.name != null }.groupBy { it.name!!.value }

    @Transient
    val allTypeElements = files.flatMap { it.typeElements }.associateBy { it.name.value }

    @Transient
    override val allTypes by lazy { allTypeElements.mapValues { it.value.asTypeDefinition } }

    @Transient
    override val scope: Scope = Scope(
        functions = allFunctions,
        typeElements = allTypeElements
    )

    @Transient
    override val exportScope: Scope = Scope(
        functions = allFunctions,//.filter { it.exported }
        typeElements = allTypeElements //.filter { it.exported }
    )



    override fun writeToFile(path: Path) {
        path.writeText(json.encodeToString(this))
    }


    fun getSymbolID(type: TypeDefinitionElement): Int {
        return typeDb.getSymbolId(type)
    }

    fun getSymbolID(type: FunctionElement): Int {
        return functionDb.getSymbolId(type)
    }

    fun getSymbolID(type: VariableElement): Int {
        return variableDb.getSymbolId(type)
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
    companion object {
        internal fun readModule(
            text: Path,
            dependencies: Iterable<Path>
        ): JitsuModule {
            val module: JitsuModule = json.decodeFromString(text.readText())
            val restoredDependencies = readDependencies(dependencies)
            restoreJitsuModule(module, restoredDependencies)
            return module
        }

        private fun readDependencies(
            dependencies: Iterable<Path>
        ): Map<String, JitsuModule> {
            val parsedDependencies = dependencies.asSequence()
                .map { it.readText() }
                .map { json.decodeFromString<JitsuModule>(it) }
                .toMutableList()
            val restoredDependencies = restoreDependencies(parsedDependencies)
            return restoredDependencies
        }

        private fun restoreDependencies(parsedDependencies: MutableList<JitsuModule>): Map<String, JitsuModule> {
            val restoredDependencies = mutableMapOf<String, JitsuModule>()
            while (parsedDependencies.isNotEmpty()) {
                val dependenciesReadyToBeRestored = parsedDependencies.filter {
                    it.dependencies.none { restoredDependencies[it] == null }
                }
                if (dependenciesReadyToBeRestored.isEmpty()) throw IllegalStateException("Circular dependency detected among ${parsedDependencies.map { it.fullyQualifiedName }}")
                dependenciesReadyToBeRestored.forEach {
                    restoreJitsuModule(it, restoredDependencies)
                    parsedDependencies.remove(it)
                    val prev = restoredDependencies.putIfAbsent(it.fullyQualifiedName, it)
                    if (prev != null) throw IllegalStateException("Duplicate dependency ${it.fullyQualifiedName}")
                }
            }
            return restoredDependencies
        }

        fun createModule(syntaxTree: JitsuModuleAst, dependencies: Collection<Path>): JitsuModulePublicApi {
            return buildJitsuModule(syntaxTree, readDependencies(dependencies))
        }
    }
}
//Kotlin bug: https://discuss.kotlinlang.org/t/polymorphic-serialization-doesnt-work-for-deeper-sealed-class-hierarchies/20534
private val polymorphicModule = SerializersModule {
    polymorphic(ExpressionElement::class) {
        subclass(FunctionCall::class)
        subclass(ArrayLiteral::class)
        subclass(UndefinedExpression::class)
        subclass(ConstantElement.BooleanConstant::class)
        subclass(ConstantElement.IntConstant::class)
        subclass(ConstantElement.StringConstant::class)
        subclass(ConstantElement.UIntConstant::class)
        subclass(VariableReference::class)
    }
}
private val json = Json {
    serializersModule = polymorphicModule
    classDiscriminator = "class"
}