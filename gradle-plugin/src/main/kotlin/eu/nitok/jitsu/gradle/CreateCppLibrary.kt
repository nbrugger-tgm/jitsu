package eu.nitok.jitsu.gradle

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurablePublishArtifact
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
import java.util.concurrent.Callable
import java.util.stream.Stream
import javax.inject.Inject

open class CppLibraryCreator @Inject constructor(
    val componentFactory: NativeComponentFactory,
    val toolChainSelector: ToolChainSelector,
    val attributesFactory: AttributesFactory,
    val targetMachineFactory: TargetMachineFactory
) {
    fun createCppLibrary(project: Project, name: String, forExternalConsumption:Boolean): CppLibrary {
        val library = componentFactory.newInstance(
            CppLibrary::class.java,
            DefaultCppLibrary::class.java,
            name
        )
        library.baseName.convention(project.project.name)
        library.targetMachines.convention(Dimensions.useHostAsDefaultTargetMachine(targetMachineFactory))
        library.developmentBinary.convention(project.project.provider<CppBinary?>(object : Callable<CppBinary?> {
            override fun call(): CppBinary? {
                return this.debugSharedHostStream.findFirst()
                    .or(this.debugStaticHostStream::findFirst)
                    .or(this.debugSharedStream::findFirst)
                    .or(this.debugStaticStream::findFirst)
                    .orElse(null)
            }

            private val debugStream: Stream<CppBinary?>
                get() = library.binaries.get().stream().filter { binary: CppBinary? -> !binary!!.isOptimized }

            private val debugSharedStream: Stream<CppBinary?>
                get() = debugStream.filter { obj -> CppSharedLibrary::class.isInstance(obj) }

            private val debugSharedHostStream: Stream<CppBinary?>
                get() = debugSharedStream.filter { binary: CppBinary? ->
                    Architectures.forInput(binary!!.targetMachine.architecture.name) == DefaultNativePlatform.host().architecture
                }

            private val debugStaticStream: Stream<CppBinary?>
                get() = debugStream.filter { obj -> CppStaticLibrary::class.isInstance(obj) }

            private val debugStaticHostStream: Stream<CppBinary?>
                get() = debugStaticStream.filter { binary: CppBinary? ->
                    Architectures.forInput(binary!!.targetMachine.architecture.name) == DefaultNativePlatform.host().architecture
                }
        }))
        library.binaries.whenElementKnown { binary -> library.mainPublication.addVariant(binary) }
        project.afterEvaluate {
            Dimensions.libraryVariants(
                library.baseName,
                library.linkage,
                library.targetMachines,
                project.objects,
                attributesFactory,
                project.providers.provider { project.project.group.toString() },
                project.providers.provider { project.project.version.toString() },
            ) { variantIdentity ->
                if (Dimensions.tryToBuildOnHost(variantIdentity)) {
                    val result: ToolChainSelector.Result<CppPlatform?> =
                        toolChainSelector.select(
                            CppPlatform::class.java,
                            DefaultCppPlatform(variantIdentity!!.targetMachine)
                        )
                    if (variantIdentity.linkage == Linkage.SHARED) {
                        library.addSharedLibrary(
                            variantIdentity,
                            result.getTargetPlatform(),
                            result.toolChain,
                            result.platformToolProvider
                        )
                    } else {
                        library.addStaticLibrary(
                            variantIdentity,
                            result.getTargetPlatform(),
                            result.toolChain,
                            result.platformToolProvider
                        )
                    }
                } else {
                    library.mainPublication.addVariant(variantIdentity)
                }
            }
            val apiElements = library.apiElements
            val publicHeaders = project.providers.provider {
                val files = library.publicHeaderDirs.files
                if (files.size != 1) {
                    throw UnsupportedOperationException(
                        String.format(
                            "The C++ library plugin currently requires exactly one public header directory, however there are %d directories configured: %s",
                            files.size,
                            files
                        )
                    )
                } else {
                    return@provider files.iterator().next()
                }
            }
            apiElements.outgoing.artifact(
                publicHeaders,
                Action { it: ConfigurablePublishArtifact? -> it!!.builtBy(*arrayOf<Any?>(library.publicHeaderDirs)) })
            if (!forExternalConsumption) {
                // Bindings are only meant to be consumed directly (by object reference) from the
                // main compilation task within this same build, not resolved as a Gradle variant.
                apiElements.isCanBeConsumed = false
            }
//            project.project.pluginManager.withPlugin("maven-publish", Action { appliedPlugin: AppliedPlugin? ->
//                val headersZip = project.tasks.register<Zip?>("cppHeaders", Zip::class.java, Action { task: Zip? ->
//                    task!!.from(*arrayOf<Any?>(library.publicHeaderFiles))
//                    task.destinationDirectory.set(project.project.layout.buildDirectory.dir("headers"))
//                    task.archiveClassifier.set("cpp-api-headers")
//                    task.archiveFileName.set("cpp-api-headers.zip")
//                })
//                headersZip.map { it }
//                library.mainPublication.addArtifact(headersZip)
//            })
            library.binaries.realizeNow()
            if (!forExternalConsumption) {
                // hiding the elements from public consumption
                library.binaries.get().forEach { binary ->
                    (binary as? ComponentWithLinkUsage)?.linkElements?.orNull?.isCanBeConsumed = false
                    (binary as? ComponentWithRuntimeUsage)?.runtimeElements?.orNull?.isCanBeConsumed = false
                }
            }
        }

        project.components.add(library)
        return library
    }
}