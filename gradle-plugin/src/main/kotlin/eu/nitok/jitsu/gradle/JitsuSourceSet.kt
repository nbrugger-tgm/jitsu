package eu.nitok.jitsu.gradle

import eu.nitok.jitsu.gradle.tasks.JitsuCompile
import eu.nitok.jitsu.gradle.tasks.JitsuTranspile
import org.gradle.api.Named
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.provider.Provider

class JitsuSourceSet(
    val sourceDirectory: SourceDirectorySet,
    val compileTask: Provider<JitsuCompile>,
    val transpileTasks: List<Provider<JitsuTranspile>>,
    val classpath: Provider<out Configuration>,
    val dependencyScope: Provider<out Configuration>,
) : Named {
    override fun getName(): String {
        return sourceDirectory.name
    }
}