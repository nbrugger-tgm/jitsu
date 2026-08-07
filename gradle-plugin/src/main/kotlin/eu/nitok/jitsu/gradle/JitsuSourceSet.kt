package eu.nitok.jitsu.gradle

import eu.nitok.jitsu.gradle.tasks.CreateModuleInfo
import eu.nitok.jitsu.gradle.tasks.JitsuCompile
import eu.nitok.jitsu.gradle.tasks.JitsuTranspile
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.internal.extensions.stdlib.capitalized
import org.gradle.language.cpp.CppComponent
import org.gradle.language.cpp.CppLibrary

open class JitsuSourceSet(
    @get:JvmName("getSourceSetName")
    val name: String,
    private val tasks: TaskContainer,
    private val configurations: ConfigurationContainer,
    objects: ObjectFactory
) : Named {

    val jistuSources: SourceDirectorySet = objects.sourceDirectorySet("jitsu$name", "$name Jitsu source").apply {
        filter.include("**/*.jit")
        srcDir("src/${this@JitsuSourceSet.name}/jitsu")
    }

    val resourceSource: SourceDirectorySet = objects.sourceDirectorySet("jitsu${name}resources", "$name Jitsu resource").apply {
        srcDir("src/${this@JitsuSourceSet.name}/resources")
    }

    lateinit var nativeCompilation: CppComponent
        internal set

    fun nativeCompilation(configure:(CppComponent.()->Unit)) {
        nativeCompilation.configure()
    }

    lateinit var nativeBindings: CppLibrary
        internal set


    fun nativeBindings(configure:(CppComponent.()->Unit)) {
        nativeBindings.configure()
    }

    val compileTaskName: String = "compile${name.capitalized()}Jitsu"
    val compileTask: TaskProvider<JitsuCompile> get() = tasks.named(compileTaskName,JitsuCompile::class.java)
    val transpileTaskName: String = "transpile${name.capitalized()}JitsuToC"
    val transpileTask: TaskProvider<JitsuTranspile> get() = tasks.named(transpileTaskName, JitsuTranspile::class.java)
    val moduleInfoTaskName: String = "createJitsu${name.capitalized()}ModuleInfo"
    val moduleInfoTask: Provider<CreateModuleInfo> get() = tasks.named(moduleInfoTaskName, CreateModuleInfo::class.java)
    val moduleClasspathName: String = "${name}JitsuModuleClasspath"
    val moduleClasspath: NamedDomainObjectProvider<out Configuration> get() = configurations.named(moduleClasspathName)
    val dependencyScopeName: String = "jitsu${name.capitalized()}"
    val dependencyScope: NamedDomainObjectProvider<out Configuration> get() = configurations.named(dependencyScopeName)
    val nativeDependencyScopeName: String = "jitsu${name.capitalized()}Bindings"
    val nativeDependencyScope: NamedDomainObjectProvider<out Configuration> get() = configurations.named(nativeDependencyScopeName)

    override fun getName(): String {
        return name
    }
}