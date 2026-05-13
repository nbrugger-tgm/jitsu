package eu.nitok.jitsu.gradle.tasks

import eu.nitok.jitsu.common.CompilerMessage
import eu.nitok.jitsu.common.format
import eu.nitok.jitsu.common.sequence
import eu.nitok.jitsu.compiler.graph.JitsuModule
import eu.nitok.jitsu.compiler.graph.buildJitsuModule
import eu.nitok.jitsu.compiler.graph.restoreJitsuModule
import eu.nitok.jitsu.gradle.json
import eu.nitok.jitsu.parser.ast.JitsuModuleAst
import eu.nitok.jitsu.parser.parseJitsuModule
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.decodeFromStream
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import javax.inject.Inject

@CacheableTask
abstract class JitsuCompile @Inject constructor() : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
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
        val dependencies = dependencies.asSequence()
            .map { json.decodeFromStream<JitsuModule>(it.inputStream()) }
            .toList()


        dependencies.forEach { restoreJitsuModule(it) }
        val graph = buildJitsuModule(moduleAst, dependencies)
        val errors = graph.messages.errors + parseErrors
        graph.messages.warnings.forEach {
            logger.warn(it.format("w"))
        }
        if(errors.isNotEmpty()) {
            errors.forEach { error ->
                logger.warn(error.format("e"))
            }
            throw GradleException("Jitsu compilation failed")
        }
        val moduleCache = targetFile.get().asFile
        moduleCache.createNewFile()
        moduleCache.writeText(json.encodeToString(graph))
        logger.info("Store module cache in $moduleCache")
    }

    protected fun parse(moduleName: String): Pair<JitsuModuleAst, List<CompilerMessage>> {
        val sourceFiles = sources.files.map { it.toPath() }.toSet()
        val ast = parseJitsuModule(sourceFiles, moduleName)
        val allAstElements = ast.allModules.flatMap { it.files }.flatMap {file -> file.sequence() }.toList()
        val allErrors = allAstElements.flatMap { it.errors }
        allAstElements.flatMap { it.warnings }.forEach {
            logger.warn(it.format("warn"))
        }
        return ast to allErrors
    }
}