plugins {
    `java-gradle-plugin`
    id("eu.nitok.jitsu.kotlin-common-conventions")
}

dependencies {
    implementation(project(":compiler"))
    implementation(project(":parser"))
    implementation(project(":backend:c"))
}

gradlePlugin {
    val jitsu_app by plugins.creating {
        id = "eu.nitok.jitsu-app"
        implementationClass = "eu.nitok.jitsu.gradle.JitsuAppPlugin"
    }
    val jitsu_lib by plugins.creating {
        id = "eu.nitok.jitsu-lib"
        implementationClass = "eu.nitok.jitsu.gradle.JitsuLibPlugin"
    }
}

// Add a source set for the functional test suite
val functionalTestSourceSet = sourceSets.create("functionalTest") {
}

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["functionalTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

// Add a task to run the functional tests
val functionalTest by tasks.registering(Test::class) {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    useJUnitPlatform()
}

gradlePlugin.testSourceSets.add(functionalTestSourceSet)

tasks.named<Task>("check") {
    dependsOn(functionalTest)
}
