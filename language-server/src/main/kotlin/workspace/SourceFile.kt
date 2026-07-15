package workspace

import eu.nitok.jitsu.parser.ast.SourceFileNode
import eu.nitok.jitsu.parser.parseJitsuFile
import helpers.VIRTUAL_THREADS
import java.net.URI
import java.util.concurrent.CompletableFuture

private val uriVersions = mutableMapOf<URI, Int>()

class SourceFile(
    val uri: URI,
    content: String
) {
    var version: Int = uriVersions.compute(uri) { _, v -> v?.plus(1) ?: 0 }!!
        private set
    var content: String = content
        private set
    var ast: CompletableFuture<out SourceFileNode> = parse(content)
        private set
    val graph get() = module!!.graph.thenApply {
        val submodulePath = uri.toString()
            .substring(module?.sourceRoot.toString().length)
            .replace("/?[^/]+\\.jit".toRegex(),"")
        var submodule = it
        if(submodulePath.isNotEmpty()) submodulePath.split("/").forEach { subModuleName ->
            submodule = submodule.submodules.first { it.name == subModuleName }
        }

        submodule.files.firstOrNull { it.uri == uri }
            ?: throw IllegalStateException("Source file $uri not found in module graph (${it.files.map { it.uri }}")
    }

    var module: WorkspaceModule? = null
    fun setContent(content: String) {
        this.content = content
        ast = parse(content)
        module?.invalidate()
        version++
        uriVersions.computeIfPresent(uri) { k, v -> v + 1 }
    }

    private fun parse(content: String): CompletableFuture<out SourceFileNode> = CompletableFuture.supplyAsync({
        parseJitsuFile(content, uri)
    }, VIRTUAL_THREADS)

    override fun toString(): String {
        return "SourceFile(file: ${uri}, version=$version)"
    }
}