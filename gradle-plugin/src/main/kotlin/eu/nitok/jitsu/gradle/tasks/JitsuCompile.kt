package eu.nitok.jitsu.gradle.tasks

import eu.nitok.jitsu.common.CompilerMessage
import eu.nitok.jitsu.common.format
import eu.nitok.jitsu.common.sequence
import eu.nitok.jitsu.compiler.graph.api.JitsuModule
import eu.nitok.jitsu.parser.ast.JitsuModuleAst
import eu.nitok.jitsu.parser.parseJitsuFile
import kotlinx.serialization.ExperimentalSerializationApi
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.nio.file.Path
import javax.inject.Inject
import kotlin.io.path.readText

@CacheableTask
abstract class JitsuCompile @Inject constructor() : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:SkipWhenEmpty
    abstract val sources: ConfigurableFileCollection

    @get:OutputFile
    abstract val targetFile: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val dependencies: ConfigurableFileCollection

    @get:Input
    abstract val moduleName: Property<String>

    @TaskAction
    @OptIn(ExperimentalSerializationApi::class)
    fun compile() {
        val (moduleAst, parseErrors) = parse(moduleName.get())

        val compilation = JitsuModule.compile(moduleAst, dependencies.files.map { it.toPath() })
        val graph = compilation.module
        val errors = compilation.messages.errors + parseErrors
        compilation.messages.warnings.forEach {
            logger.warn(it.format("w"))
        }
        if (errors.isNotEmpty()) {
            errors.forEach { error ->
                logger.warn(error.format("e"))
            }
            throw GradleException("Jitsu compilation failed")
        }
        val moduleCache = targetFile.get().asFile
        moduleCache.createNewFile()
        graph.writeToFile(moduleCache.toPath())
        logger.info("Store module cache in $moduleCache")
        logger.lifecycle("Compile Jitsu with dependencies: ${dependencies.files.joinToString(", ")}")
    }
    class ModuleDirectory(var name: String, var subModules: MutableList<ModuleDirectory>, var files: MutableList<Path>)
    protected fun parse(moduleName: String): Pair<JitsuModuleAst, List<CompilerMessage>> {
        val rootMod = getSourceModules(moduleName)
        fun parse(folder: ModuleDirectory):JitsuModuleAst = JitsuModuleAst(
            folder.name,
            folder.files.map { parseJitsuFile(it.readText(), it.toUri()) },
            folder.subModules.map { parse(it) }
        )
        val ast = parse(rootMod)
        val allAstElements = ast.allModules.flatMap { it.files }.flatMap { file -> file.sequence() }.toList()
        val allErrors = allAstElements.flatMap { it.errors }
        allAstElements.flatMap { it.warnings }.forEach {
            logger.warn(it.format("warn"))
        }
        return ast to allErrors
    }

    private fun getSourceModules(moduleName: String): ModuleDirectory {
        val rootMod = ModuleDirectory(moduleName, mutableListOf(), mutableListOf())
        run {
            val mods = mutableMapOf<String, ModuleDirectory>()
            sources.asFileTree.visit {
                if (it.isDirectory) {
                    val mod = mods.getOrPut(it.path) {
                        ModuleDirectory(it.name, mutableListOf(), mutableListOf())
                    }
                    (mods[it.relativePath.parent.pathString] ?: rootMod).subModules.add(mod)
                } else {
                    (mods[it.relativePath.parent.pathString] ?: rootMod).files.add(it.file.toPath())
                }
            }
        }
        return rootMod
    }
}