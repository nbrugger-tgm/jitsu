package eu.nitok.jitsu.compiler.cli.commands

import eu.nitok.jitsu.compiler.ast.Located
import eu.nitok.jitsu.compiler.graph.JitsuFile
import eu.nitok.jitsu.compiler.graph.buildGraph
import eu.nitok.jitsu.compiler.model.flatMap
import kotlinx.serialization.encodeToString
import picocli.CommandLine.*
import picocli.CommandLine.Model.CommandSpec
import java.nio.file.Path
import java.util.concurrent.Callable
import kotlin.io.path.*

@Command(
    name = "process",
    description = ["processes sourcecode and builds an internal graph"]
)
class Process : Callable<List<Pair<JitsuFile, Path>>> {
    @Mixin
    lateinit var cli: Parse;

    @Spec
    lateinit var spec: CommandSpec
    override fun call(): List<Pair<JitsuFile, Path>> {
        val scopes = cli.call().map {
            buildGraph(it.first) to it.second
        }
        cli.cacheDirectory.ensureExistingDir();
        val graphCache = cli.cacheDirectory.resolve("graph").ensureExistingDir()
        scopes.forEach {
            val cacheFile = graphCache.resolve("${it.second.nameWithoutExtension}.graph.json").ensureExistingFile()
            cacheFile.writeText(
                cli.json.encodeToString(it.first)
            )
            spec.commandLine().out.println("Write graph to $cacheFile")

            val errors = it.first.scope.flatMap { it.errors }
            cli.printErrors(errors, it.second)
        }
        return scopes
    }
}