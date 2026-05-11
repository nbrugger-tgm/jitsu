package eu.nitok.jitsu.gradle

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

abstract class JitsuExtension @Inject constructor(
    objects: ObjectFactory
) {
    val sourceSets: NamedDomainObjectContainer<JitsuSourceSet> =
        objects.domainObjectContainer(JitsuSourceSet::class.java) { name ->
            objects.newInstance(JitsuSourceSet::class.java, name)
        }
}