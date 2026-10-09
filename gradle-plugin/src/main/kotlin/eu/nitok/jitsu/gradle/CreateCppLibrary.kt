package eu.nitok.jitsu.gradle

import org.gradle.api.Project
import org.gradle.api.internal.attributes.AttributesFactory
import org.gradle.language.cpp.*
import org.gradle.language.cpp.internal.DefaultCppLibrary
import org.gradle.language.cpp.internal.DefaultCppPlatform
import org.gradle.language.internal.NativeComponentFactory
import org.gradle.language.nativeplatform.ComponentWithLinkUsage
import org.gradle.language.nativeplatform.ComponentWithRuntimeUsage
import org.gradle.language.nativeplatform.internal.Dimensions
import org.gradle.language.nativeplatform.internal.toolchains.ToolChainSelector
import org.gradle.nativeplatform.Linkage
import org.gradle.nativeplatform.TargetMachineFactory
import org.gradle.nativeplatform.platform.internal.Architectures
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import java.io.File
import java.util.concurrent.Callable
import java.util.stream.Stream
import javax.inject.Inject

/**
 * Creates [CppLibrary] components the way `CppLibraryPlugin` does, but without being a plugin.
 *
 * Gradle's `cpp-library` plugin hard codes a single component named `main` per project and offers
 * no public API to add further ones. A Jitsu project needs several (the module itself plus one
 * bindings library per source set), so this is a port of
 * `org.gradle.language.cpp.plugins.CppLibraryPlugin` - **copied & adapted from Gradle 9.8.1**.
 *
 * Differences to the original:
 *  * the component name is a parameter instead of the constant `"main"`,
 *  * the component is not registered as the `library` project extension,
 *  * [publishedCapability] can move the outgoing variants off the project's default capability,
 *  * the `maven-publish` headers zip is not registered.
 *
 * When upgrading Gradle, diff this against the upstream file of the matching tag; it relies on
 * several `@Internal` types ([NativeComponentFactory], [Dimensions], [DefaultCppLibrary],
 * [AttributesFactory]) that are not covered by any compatibility guarantee.
 *
 * Known limitation: `maven-publish` cannot be applied to a project that owns more than one
 * [CppLibrary], because Gradle's `NativeBasePlugin` derives the publication name from the
 * component and the second one collides ("Cannot add a Publication with name 'main' as a
 * Publication with that name already exists"). Jitsu projects therefore cannot be published to a
 * repository yet; composite/project dependencies work. This predates the Gradle 9 upgrade.
 */
open class CppLibraryCreator @Inject constructor(
    private val componentFactory: NativeComponentFactory,
    private val toolChainSelector: ToolChainSelector,
    private val attributesFactory: AttributesFactory,
    private val targetMachineFactory: TargetMachineFactory
) {
    /**
     * @param name name of the component, drives task and configuration names
     * @param publishedCapability feature name whose capability replaces the default capability on
     *   all outgoing variants. Variants with a non default capability are invisible to consumers
     *   that do not explicitly require them, which is how the bindings library is kept out of the
     *   way of plain C/C++ consumers.
     */
    fun createCppLibrary(
        project: Project,
        name: String,
        publishedCapability: String? = null
    ): CppLibrary {
        val library = componentFactory.newInstance(CppLibrary::class.java, DefaultCppLibrary::class.java, name)

        library.baseName.convention(project.name)
        library.targetMachines.convention(Dimensions.useHostAsDefaultTargetMachine(targetMachineFactory))
        library.developmentBinary.convention(project.provider(object : Callable<CppBinary?> {
            override fun call(): CppBinary? = debugSharedHostStream.findFirst()
                .or(debugStaticHostStream::findFirst)
                .or(debugSharedStream::findFirst)
                .or(debugStaticStream::findFirst)
                .orElse(null)

            private val debugStream: Stream<CppBinary>
                get() = library.binaries.get().stream().filter { !it.isOptimized }

            private val debugSharedStream: Stream<CppBinary>
                get() = debugStream.filter(CppSharedLibrary::class.java::isInstance)

            private val debugSharedHostStream: Stream<CppBinary>
                get() = debugSharedStream.filter { it.isHostArchitecture }

            private val debugStaticStream: Stream<CppBinary>
                get() = debugStream.filter(CppStaticLibrary::class.java::isInstance)

            private val debugStaticHostStream: Stream<CppBinary>
                get() = debugStaticStream.filter { it.isHostArchitecture }
        }))

        library.binaries.whenElementKnown { library.mainPublication.addVariant(it) }

        val capability = publishedCapability?.let { project.featureCapability(it) }

        project.afterEvaluate {
            Dimensions.libraryVariants(
                library.baseName,
                library.linkage,
                library.targetMachines,
                attributesFactory,
                project.providers.provider { project.group.toString() },
                project.providers.provider { project.version.toString() },
            ) { variantIdentity ->
                if (!Dimensions.tryToBuildOnHost(variantIdentity)) {
                    // known, but not buildable
                    library.mainPublication.addVariant(variantIdentity)
                    return@libraryVariants
                }
                val result = toolChainSelector.select(
                    CppPlatform::class.java,
                    DefaultCppPlatform(variantIdentity.targetMachine)
                )
                if (variantIdentity.linkage == Linkage.SHARED) {
                    library.addSharedLibrary(
                        variantIdentity, result.targetPlatform, result.toolChain, result.platformToolProvider
                    )
                } else {
                    library.addStaticLibrary(
                        variantIdentity, result.targetPlatform, result.toolChain, result.platformToolProvider
                    )
                }
            }

            // TODO - deal with more than one header dir, e.g. generated public headers
            val publicHeaders = project.providers.provider {
                val files: Set<File> = library.publicHeaderDirs.files
                if (files.size != 1) {
                    throw UnsupportedOperationException(
                        "The C++ library plugin currently requires exactly one public header directory, " +
                                "however there are ${files.size} directories configured: $files"
                    )
                }
                files.first()
            }
            library.apiElements.configure { apiElements ->
                apiElements.outgoing.artifact(publicHeaders) { it.builtBy(library.publicHeaderDirs) }
                capability?.let { apiElements.outgoing.capability(it) }
            }

            library.binaries.realizeNow()

            if (capability != null) {
                library.binaries.get().forEach { binary ->
                    (binary as? ComponentWithLinkUsage)?.linkElements?.orNull?.outgoing?.capability(capability)
                    (binary as? ComponentWithRuntimeUsage)?.runtimeElements?.orNull?.outgoing?.capability(capability)
                }
            }
        }

        project.components.add(library)
        return library
    }

    private val CppBinary.isHostArchitecture: Boolean
        get() = Architectures.forInput(targetMachine.architecture.name) == DefaultNativePlatform.host().architecture
}
