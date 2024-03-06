package eu.nitok.jitsu.compiler.cli.commands

import eu.nitok.jitsu.compiler.ast.SourceFileNode
import eu.nitok.jitsu.compiler.cli.Jitsu
import eu.nitok.jitsu.compiler.parser.parseFile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.modules.EmptySerializersModule
import picocli.CommandLine
import picocli.CommandLine.*
import picocli.CommandLine.Model.CommandSpec
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.Callable
import kotlin.io.path.*


@Command(
    name = "parse",
    description = ["parses sourcecode into a AST and caches it as json"]
)
class Parse : Callable<List<Pair<SourceFileNode, Path>>> {
    @ParentCommand
    lateinit var cli: Jitsu;
    @Spec
    lateinit var spec: CommandSpec

    @Parameters(description = ["All source-files to process"])
    var sourcepath: List<Path> = listOf()

    @Option(names = ["-o", "--output"])
    var outpurDir = Paths.get("build")
    val cacheDirectory get() = outpurDir.resolve("cache")

    val json: Json = Json(
        builderAction = {
            prettyPrint = true;
            classDiscriminator = "class"
        }
    );

    override fun call(): List<Pair<SourceFileNode, Path>> {
        spec.commandLine().out.println("Compile : ${sourcepath}")
        val asts = sourcepath.map {
            parseFile(it.bufferedReader(bufferSize = 100 * 40), it.toUri()) to it
        }
        if (!cacheDirectory.exists()) cacheDirectory.createParentDirectories().createDirectory()
        val parsedCache = cacheDirectory.resolve("ast")
        if (!parsedCache.exists()) parsedCache.createDirectory()
        asts.forEach {
            val cache = parsedCache.resolve("${it.second.nameWithoutExtension}.ast.json");
            if (!cache.exists()) cache.createFile()
            cache.writeText(json.encodeToString(it.first))
            spec.commandLine().out.println("Write ast cache to ${cache.absolutePathString()}")
        }
        val errors = asts.map {
            it.first.flatMap {
                it.errors
            } to it.second
        }
        errors.forEach {
            val fileContent = lazy { it.second.readText() }
            it.first.forEach {
                val errorMark = it.location.mark(fileContent.value, it.message)
                spec.commandLine().err.println(errorMark)
                it.hints.forEach {
                    val hintMark = it.location.mark(fileContent.value, it.message).replace("\n", "\n\t|")
                    spec.commandLine().err.println(hintMark)
                }
            }
            if (it.first.isNotEmpty()) throw IllegalStateException()
        }
        return asts;
    }
}