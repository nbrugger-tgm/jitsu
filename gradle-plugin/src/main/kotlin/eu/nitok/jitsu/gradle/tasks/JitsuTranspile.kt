package eu.nitok.jitsu.gradle.tasks

import eu.nitok.jitsu.backend.c.CBackend
import eu.nitok.jitsu.common.format
import eu.nitok.jitsu.compiler.bitcode.lower
import eu.nitok.jitsu.compiler.graph.JitsuModule
import eu.nitok.jitsu.compiler.graph.restoreJitsuModule
import eu.nitok.jitsu.compiler.transpile.Backend
import eu.nitok.jitsu.gradle.json
import kotlinx.serialization.json.decodeFromStream
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import javax.inject.Inject

@CacheableTask
abstract class JitsuTranspile @Inject constructor() : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val dependencies: ConfigurableFileCollection

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:SkipWhenEmpty
    abstract val moduleFile: RegularFileProperty

    @get:OutputDirectory
    abstract val targetDirectory: DirectoryProperty

    @TaskAction
    fun transpile() {
        val restoredDependencies = dependencies.files.map {
            val module = json.decodeFromStream<JitsuModule>(it.inputStream())
            restoreJitsuModule(module, emptyList()/*TODO: transitive dependencies */)
            module.messages.errors.forEach { error ->
                logger.error(error.format("ERROR"))
            }
            module
        }.toList()
        val module = json.decodeFromStream<JitsuModule>(moduleFile.get().asFile.inputStream())
        restoreJitsuModule(module, restoredDependencies)
        module.messages.errors.forEach { error ->
            logger.error(error.format("ERROR"))
        }
        val modules = (restoredDependencies+module).map { file -> file.lower() }
        val files = CBackend().run {
            transpile(modules, targetDirectory.get().asFile.toPath())
        }
        logger.info("Transpiled to $files")
    }
}