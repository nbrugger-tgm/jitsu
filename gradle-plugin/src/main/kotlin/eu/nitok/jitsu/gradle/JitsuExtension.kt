package eu.nitok.jitsu.gradle

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

interface JitsuExtension {
    val moduleName: Property<String>
    val sourceSets: NamedDomainObjectContainer<JitsuSourceSet>
}
