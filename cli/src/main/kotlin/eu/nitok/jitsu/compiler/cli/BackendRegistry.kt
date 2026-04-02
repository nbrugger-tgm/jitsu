package eu.nitok.jitsu.compiler.cli

import eu.nitok.jitsu.compiler.transpile.Backend

object BackendRegistry {
    private val backends: Map<String, Backend> = mapOf()

    fun create(name: String): Backend {
        return backends[name] ?: throw IllegalArgumentException("No backend with name $name")
    }
}