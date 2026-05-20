package eu.nitok.jitsu.gradle.tasks

import eu.nitok.jitsu.common.ModuleInfo
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File
import javax.inject.Inject

abstract class CreateModuleInfo @Inject constructor(layout: ProjectLayout): DefaultTask() {
    @get:Input//not a Dir/File/Files since we care about path not content
    abstract val moduleDir: Property<File>
    @get:Input
    abstract val moduleName: Property<String>
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init {
        outputFile.convention(moduleName.flatMap { layout.buildDirectory.file("modules/module-info.$it.json") })
        onlyIf { moduleDir.orNull?.exists() ?: false }
    }

    @TaskAction
    fun create() {
        val moduleInfo = ModuleInfo(
            name = moduleName.get(),
            sourceRoot = moduleDir.get().toPath()
        )
        moduleInfo.save(outputFile.get().asFile.toPath())
    }
}