package eu.nitok.jitsu.gradle

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

val MAIN_SOURCESET_NAME = "main"
val TEST_SOURCESET_NAME = "test"
interface JitsuExtension {
    val moduleName: Property<String>
    val sourceSets: NamedDomainObjectContainer<JitsuSourceSet>
}
val NamedDomainObjectContainer<JitsuSourceSet>.main get() = named(MAIN_SOURCESET_NAME)
val NamedDomainObjectContainer<JitsuSourceSet>.test get() = named(TEST_SOURCESET_NAME)
fun NamedDomainObjectContainer<JitsuSourceSet>.main(action: JitsuSourceSet.()->Unit) = named(MAIN_SOURCESET_NAME).configure(action)
fun NamedDomainObjectContainer<JitsuSourceSet>.test(action: JitsuSourceSet.()->Unit) = named(TEST_SOURCESET_NAME).configure(action)

