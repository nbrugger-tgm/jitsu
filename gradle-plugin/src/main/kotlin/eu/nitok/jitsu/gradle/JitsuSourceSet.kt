package eu.nitok.jitsu.gradle

import eu.nitok.jitsu.gradle.tasks.CreateModuleInfo
import eu.nitok.jitsu.gradle.tasks.JitsuCompile
import eu.nitok.jitsu.gradle.tasks.JitsuTranspile
import org.gradle.api.Named
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

class JitsuSourceSet(
    val jistuSources: SourceDirectorySet,
    val resourceSource: SourceDirectorySet,
    val compileTask: TaskProvider<JitsuCompile>,
    val transpileTasks: List<TaskProvider<JitsuTranspile>>,
    val moduleInfoTask: Provider<CreateModuleInfo>,
    val classpath: Provider<out Configuration>,
    val nativeBindingsClasspath: Provider<out Configuration>,
    val dependencyScope: Provider<out Configuration>,
    val consumable: Boolean
) : Named {
    override fun getName(): String {
        return jistuSources.name
    }
}