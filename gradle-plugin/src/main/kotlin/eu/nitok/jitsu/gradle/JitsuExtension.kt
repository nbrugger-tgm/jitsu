package eu.nitok.jitsu.gradle

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.language.cpp.CppLibrary
import javax.inject.Inject

abstract class JitsuExtension @Inject constructor(
    objects: ObjectFactory
) {
    abstract val moduleName: Property<String>
    val sourceSets: NamedDomainObjectContainer<JitsuSourceSet> =
        objects.domainObjectContainer(JitsuSourceSet::class.java) { name ->
            objects.newInstance(JitsuSourceSet::class.java, name)
        }
    lateinit var nativeCompilation: CppLibrary
        internal set
    fun nativeCompilation(configure:(CppLibrary.()->Unit)) {
        nativeCompilation.configure()
    }
}