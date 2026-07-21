package eu.nitok.jitsu.compiler.graph.api

import eu.nitok.jitsu.parser.ast.JitsuModuleAst
import kotlinx.serialization.Transient
import java.nio.file.Path
import eu.nitok.jitsu.compiler.graph.elements.JitsuModule.Companion as JitsuModuleImpl

interface JitsuModule : Element {
    /**
     * Full qualified name
     */
    val fullyQualifiedName: String
    val name get() = fullyQualifiedName.substringAfterLast('.')
    val files: List<JitsuFile>
    val submodules: List<JitsuModule>
    val dependencies: Sequence<String> get() = files.asSequence().flatMap { it.imports }.map { it.name.value }
    val allDependencies: Sequence<String>
        get() = (dependencies + submodules.asSequence()
            .flatMap { it.allDependencies }
            .distinct()).filter { dependency ->
                submodules.none { it.fullyQualifiedName == dependency }
            }

    val allModules: Sequence<JitsuModule> get() = sequenceOf(this) + submodules.flatMap { it.allModules }
    val moduleLookup: Map<String, JitsuModule>

    @Transient
    val allFunctions: Map<String, List<Function>>

    @Transient
    val allTypes: Map<String, TypeDefinition>

    @Transient
    val scope: Scope

    @Transient
    val exportScope: Scope

    fun writeToFile(path: Path)

    companion object {
        fun readModule(moduleFile: Path, dependencies: Iterable<Path>): JitsuModuleResult {
            return JitsuModuleImpl.readModule(moduleFile, dependencies)
        }

        fun compile(syntaxTree: JitsuModuleAst, dependencies: Collection<Path>): JitsuModuleResult {
            return JitsuModuleImpl.createModule(syntaxTree, dependencies)
        }
    }
}