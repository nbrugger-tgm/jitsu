package workspace

import eu.nitok.jitsu.compiler.graph.api.JitsuModule
import eu.nitok.jitsu.parser.ast.JitsuModuleAst
import eu.nitok.jitsu.parser.ast.SourceFileNode
import helpers.Cache
import helpers.joinAll
import java.net.URI
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture

class WorkspaceModule(
    val name: String,
    val sourceRoot: URI,
    files: List<SourceFile> = listOf()
) {
    init {
        files.forEach { file -> file.module = this }
    }
    private val files: MutableList<SourceFile> = files.toMutableList()
    val sourceFiles: List<SourceFile> get() = files

    fun addFile(file: SourceFile) {
        file.module = this
        files.add(file)
        astCache.invalidate()
    }
    fun removeFile(uri: URI) {
        files.removeIf { it.uri == uri }
        astCache.invalidate()
    }

    private class ModuleDirectory(
        val name: String,
        val subModules: MutableList<ModuleDirectory> = mutableListOf(),
        var files: CompletableFuture<out List<SourceFileNode>>? = null
    ) {
        fun toAst(): CompletableFuture<out JitsuModuleAst> {
            val submodulesFuture = subModules.map { it.toAst() }.joinAll()
            return (files ?: completedFuture(emptyList())).thenCombine(submodulesFuture) { files, subModules ->
                JitsuModuleAst(
                    name = name,
                    files = files,
                    modules = subModules
                )
            }
        }
    }

    private val astCache = Cache {
        val filesByModule = groupFilesByPath()
        val modules = createModuleStructure(filesByModule)
        val module = modules[name]?:throw IllegalStateException("Module $name not found ($modules)")
        module.toAst()
    }
    val ast: CompletableFuture<out JitsuModuleAst> get() = astCache.get()


    internal fun invalidate() {
        astCache.invalidate()
        graphCache.invalidate()
    }
    private val graphCache = Cache {
        ast.thenApply {
            JitsuModule.compile(it,listOf(/*TODO dependency support*/))
        }
    }
    val graph get() = graphCache.get()

    private fun createModuleStructure(filesByModule: Map<String, CompletableFuture<out List<SourceFileNode>>>): MutableMap<String, ModuleDirectory> {
        val modules = mutableMapOf<String, ModuleDirectory>()
        fun getModule(path: String): ModuleDirectory {
            val existingModule = modules[path]
            if (existingModule != null) return existingModule;
            val (parentModulePath, moduleName) = parseModulePath(path)
            val module = ModuleDirectory(name = moduleName)
            modules[path] = module
            if (parentModulePath != null) {
                val parent = getModule(parentModulePath)
                parent.subModules.add(module)
            }
            return module
        }
        filesByModule.forEach { (modulePath, files) ->
            val module = getModule(modulePath)
            module.files = files
        }
        return modules
    }

    private fun parseModulePath(path: String): Pair<String?, String> {
        val slashIndex = path.lastIndexOf('/')
        val parentPath = if (slashIndex > -1) path.substring(0, slashIndex) else null
        val moduleName = path.substring(slashIndex + 1)
        return parentPath to moduleName
    }

    private fun groupFilesByPath(): Map<String, CompletableFuture<out List<SourceFileNode>>> = files.map {
        val fileUri = it.uri.toString()
        val relativeUri = fileUri.replace(sourceRoot.toString(), "$name/")
        val subModulePath = relativeUri.substring(0,relativeUri.lastIndexOf('/'))
        subModulePath to it.ast
    }.groupBy({ it.first }, { it.second }).mapValues { (_, v) -> v.joinAll() }

    override fun toString(): String {
        return "WorkspaceModule[$name,$sourceRoot,files: $sourceFiles]"
    }
}