package eu.nitok.jitsu.compiler.cli.commands

import eu.nitok.jitsu.compiler.graph.Scope
import eu.nitok.jitsu.compiler.graph.buildGraph
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import picocli.CommandLine.*
import picocli.CommandLine.Model.CommandSpec
import java.nio.file.Path
import java.util.concurrent.Callable
import kotlin.io.path.*

@Command(
    name = "process",
    description = ["processes sourcecode and builds an internal graph"]
)
class Process : Callable<List<Pair<Scope, Path>>> {
    @Mixin
    lateinit var cli: Parse;

    @Spec
    lateinit var spec: CommandSpec
    override fun call(): List<Pair<Scope, Path>> {
        val scopes = cli.call().map {
            buildGraph(it.first) to it.second
        }
        cli.cacheDirectory.ensureExistingDir();
        val graphCache = cli.cacheDirectory.resolve("graph").ensureExistingDir()
        scopes.forEach {
            val cacheFile = graphCache.resolve("${it.second.nameWithoutExtension}.graph.json").ensureExistingFile()
            cacheFile.writeText(
                Json.encodeToString(it)
            )
            spec.commandLine().out.println("Write graph to $cacheFile")
        }
        return scopes
    }

    private fun Path.ensureExistingDir(): Path {
        if (!exists()) createParentDirectories().createDirectory()
        return this
    }

    private fun Path.ensureExistingFile(): Path {
        if (!exists()) createParentDirectories().createFile()
        return this
    }
}