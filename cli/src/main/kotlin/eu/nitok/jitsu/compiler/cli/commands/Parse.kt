package eu.nitok.jitsu.compiler.cli.commands

import eu.nitok.jitsu.parser.ast.SourceFileNode
import eu.nitok.jitsu.compiler.cli.Jitsu
import eu.nitok.jitsu.common.flatMap
import eu.nitok.jitsu.common.format
import eu.nitok.jitsu.parser.parseJitsuFile
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import picocli.CommandLine.*
import picocli.CommandLine.Model.CommandSpec
import java.nio.file.Path
import java.util.concurrent.Callable
import kotlin.io.path.*
import kotlin.system.exitProcess


@Command(
    name = "parse",
    description = ["parses sourcecode into a AST and caches it as json"]
)
class Parse : Callable<List<Pair<SourceFileNode, Path>>> {
    @ParentCommand
    lateinit var cli: Jitsu;
    @Spec
    lateinit var spec: CommandSpec

    @Parameters(description = ["All source-files/directories to process"])
    var sourcepath: List<Path> = listOf()

    @Option(names = ["-o", "--output"], defaultValue = "build")
    lateinit var outputDir: Path
    val cacheDirectory get() = outputDir.resolve("cache")

    @OptIn(ExperimentalSerializationApi::class)
    val json: Json = Json(
        builderAction = {
            prettyPrint = true;
            classDiscriminator = "class"
            namingStrategy = JsonNamingStrategy { _, _, serialName -> serialName.split(".").last() }
        }
    );

    override fun call(): List<Pair<SourceFileNode, Path>> {
        spec.commandLine().out.println("Parse : $sourcepath")
        val asts = sourcepath.map {
            parseJitsuFile(it.readText(), it.toUri()) to it
        }
        asts.forEach {
            val errors = it.first.flatMap { it.errors }
            errors.forEach {
                spec.commandLine().err.print(it.format("ERROR"))
            }
            it.first.flatMap { it.warnings }.forEach {
                spec.commandLine().out.print(it.format("warn"))
            }
            if(errors.isNotEmpty()) exitProcess(1)
        }
        return asts;
    }
}

fun Path.ensureExistingDir(): Path {
    if (!exists()) createParentDirectories().createDirectory()
    return this
}

fun Path.ensureExistingFile(): Path {
    if (!exists()) createParentDirectories().createFile()
    return this
}