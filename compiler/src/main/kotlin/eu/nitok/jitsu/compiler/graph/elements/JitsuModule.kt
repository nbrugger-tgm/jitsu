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
import eu.nitok.jitsu.compiler.graph.api.JitsuModuleResult as JitsuModuleResultPublicApi

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
    @Transient private val attributeDb: IrStore<AttributeDefinitionElement> = IrStore()

    init {
        fun informElements(elem: List<Element>) {
            elem.forEach { element ->
                if (element is ModuleAware) element.setEnclosingModule(this)
                if (element !is JitsuModule) informElements(element.children)
            }
        }
        informElements(children)
    }
    override val children: List<Element> get() = files + submodules

    override val allModules: Sequence<JitsuModule> get() = sequenceOf(this) + submodules.flatMap { it.allModules }
    override val moduleLookup: Map<String, JitsuModule> by lazy { allModules.associateBy { it.fullyQualifiedName } }

    @Transient
    override val allFunctions = files.flatMap { it.functions }.filter { it.name != null }.groupBy { it.name!!.value }

    @Transient
    val allTypeElements = files.flatMap { it.typeElements }.associateBy { it.name.value }
    @Transient
    val allAttributes = files.flatMap { it.attributes }.associateBy { it.name.value }

    @Transient
    override val allTypes by lazy { allTypeElements.mapValues { it.value.asTypeDefinition } }

    @Transient
    override val scope: Scope = Scope(
        functions = allFunctions,
        typeElements = allTypeElements,
        attributes = allAttributes
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

    fun getSymbolID(type: AttributeDefinitionElement): Int {
        return attributeDb.getSymbolId(type)
    }

    fun getType(id: Int) = typeDb[id]
    fun getFunction(id: Int) = functionDb[id]
    fun getVariable(id: Int) = variableDb[id]
    fun getAttribute(id: Int) = attributeDb[id]
    companion object {
        internal fun readModule(
            text: Path,
            dependencies: Iterable<Path>
        ): JitsuModuleResultPublicApi {
            val module: JitsuModule = json.decodeFromString(text.readText())
            val (restoredDependencies, dependencyMessages) = readDependencies(dependencies)
            val messages = restoreJitsuModule(module, restoredDependencies)
            messages.add(dependencyMessages)
            return JitsuModuleResultPublicApi(module, messages)
        }

        private fun readDependencies(
            dependencies: Iterable<Path>
        ): Pair<Map<String, JitsuModule>, CompilerMessages> {
            val parsedDependencies = dependencies.asSequence()
                .map { it.readText() }
                .map { json.decodeFromString<JitsuModule>(it) }
                .toMutableList()
            val restoredDependencies = restoreDependencies(parsedDependencies)
            return restoredDependencies
        }

        private fun restoreDependencies(parsedDependencies: List<JitsuModule>): Pair<Map<String, JitsuModule>, CompilerMessages> {
            val dependenciesToRestore = parsedDependencies.flatMap { it.allModules }.toMutableList()
            val restoredDependencies = mutableMapOf<String, JitsuModule>()
            val messages = CompilerMessages()
            while (dependenciesToRestore.isNotEmpty()) {
                val dependenciesReadyToBeRestored = dependenciesToRestore.filter { toRestore ->
                    toRestore.dependencies.none { restoredDependencies[it] == null }
                }
                if (dependenciesReadyToBeRestored.isEmpty()) throw IllegalStateException("Circular dependency detected among ${parsedDependencies.map { it.fullyQualifiedName }}")
                dependenciesReadyToBeRestored.forEach {
                    messages.add(restoreJitsuModule(it, restoredDependencies))
                    dependenciesToRestore.remove(it)
                    val prev = restoredDependencies.putIfAbsent(it.fullyQualifiedName, it)
                    if (prev != null) throw IllegalStateException("Duplicate dependency ${it.fullyQualifiedName}")
                }
            }
            return restoredDependencies to messages
        }

        fun createModule(syntaxTree: JitsuModuleAst, dependencies: Collection<Path>): JitsuModuleResultPublicApi {
            val (parsedDependencies, dependencyMessages) = readDependencies(dependencies)
            val result = buildJitsuModule(syntaxTree, parsedDependencies)
            result.messages.add(dependencyMessages)
            return JitsuModuleResultPublicApi(result.module, result.messages)
        }
    }

    override fun toString(): String {
        return "JitsuMldule($fullyQualifiedName)"
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