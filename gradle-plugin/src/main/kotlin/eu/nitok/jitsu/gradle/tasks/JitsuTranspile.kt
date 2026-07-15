package eu.nitok.jitsu.gradle.tasks

import eu.nitok.jitsu.backend.c.CBackend
import eu.nitok.jitsu.common.format
import eu.nitok.jitsu.compiler.bitcode.lower
import eu.nitok.jitsu.compiler.graph.api.JitsuModule
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import javax.inject.Inject

@CacheableTask
abstract class JitsuTranspile @Inject constructor(
    private val files: FileSystemOperations
) : DefaultTask() {
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
        val module = JitsuModule.readModule(moduleFile.get().asFile.toPath(), dependencies.files.map { it.toPath() })
        module.messages.errors.forEach { error ->
            logger.error(error.format("e"))
        }
        val modules = module.lower()
        val files = CBackend().run {
            val outputDir = targetDirectory.get().asFile
            files.delete {
                it.delete(outputDir)
            }
            transpile(listOf(modules), outputDir.toPath())
        }
        logger.info("Transpiled to $files")
    }
}