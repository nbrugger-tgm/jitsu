plugins {
    id("eu.nitok.jitsu.kotlin-library-conventions")
}

dependencies {
    api(project(":compiler-utils"))
    api("com.niton.jainparse:tokenizer")
}
