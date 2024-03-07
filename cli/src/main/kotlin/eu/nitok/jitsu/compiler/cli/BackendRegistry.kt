package eu.nitok.jitsu.compiler.cli

import eu.nitok.jitsu.backend.rust.eu.nitok.jitsu.backend.rust.RustBackend
import eu.nitok.jitsu.compiler.transpile.Backend

object BackendRegistry {
    private val backends: Map<String, Backend> = mapOf(
        "rust" to RustBackend()
    )

    fun create(name: String): Backend {
        return backends[name] ?: throw IllegalArgumentException("No backend with name $name")
    }
}