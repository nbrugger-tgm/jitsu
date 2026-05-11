package eu.nitok.jitsu.compiler.cli.commands

import eu.nitok.jitsu.common.format
import eu.nitok.jitsu.compiler.graph.JitsuModule
import eu.nitok.jitsu.compiler.graph.buildJitsuModule
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
class Process : Callable<List<Pair<JitsuModule, Path>>> {
    @Mixin
    lateinit var cli: Parse;

    @Spec
    lateinit var spec: CommandSpec
    override fun call(): List<Pair<JitsuModule, Path>> {
        val scopes = cli.call().map {
            buildJitsuModule(it.first) to it.second
        }
        cli.cacheDirectory.ensureExistingDir();
        val graphCache = cli.cacheDirectory.resolve("graph").ensureExistingDir()
        scopes.forEach {
            val cacheFile = graphCache.resolve("${it.second.nameWithoutExtension}.graph.json").ensureExistingFile()
            cacheFile.writeText(
                cli.json.encodeToString(it.first)
            )
            spec.commandLine().out.println("Write graph to $cacheFile")

            val errors = it.first.messages.errors
            errors.forEach {
                cli.spec.commandLine().err.println(it.format("ERROR"))
            }
            if(errors.isNotEmpty()) {
                throw IllegalStateException("Errors found in ${it.second}")
            }
        }
        return scopes
    }
}