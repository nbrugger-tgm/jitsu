package eu.nitok.jitsu.compiler.cli.commands

import eu.nitok.jitsu.compiler.ast.SourceFileNode
import eu.nitok.jitsu.compiler.cli.Jitsu
import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage
import eu.nitok.jitsu.compiler.model.flatMap
import eu.nitok.jitsu.compiler.parser.parseFile
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.modules.EmptySerializersModule
import picocli.CommandLine
import picocli.CommandLine.*
import picocli.CommandLine.Model.CommandSpec
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.Callable
import java.util.regex.Pattern
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

    @OptIn(ExperimentalSerializationApi::class)
    val json: Json = Json(
        builderAction = {
            prettyPrint = true;
            classDiscriminator = "class"
            namingStrategy = JsonNamingStrategy { _, _, serialName -> serialName.split(".").last() }
        }
    );

    override fun call(): List<Pair<SourceFileNode, Path>> {
        spec.commandLine().out.println("Compile : $sourcepath")
        val asts = sourcepath.map {
            parseFile(it.bufferedReader(bufferSize = 100 * 40), it.toUri()) to it
        }
        val parsedCache = cacheDirectory.resolve("ast").ensureExistingDir()
        asts.forEach {
            val cache = parsedCache.resolve("${it.second.nameWithoutExtension}.ast.json").ensureExistingFile()
            cache.writeText(json.encodeToString(it.first))
            spec.commandLine().out.println("Write ast cache to ${cache.absolutePathString()}")
        }
        asts.forEach {
            printErrors(it.first.flatMap { it.errors }, it.second)
        }
        return asts;
    }

    fun printErrors(errors: List<CompilerMessage>, file: Path) {
        errors.forEach {
            val fileContent = lazy { file.readText() }
            spec.commandLine().err.println(errors.joinToString("\n------------------------\n\n") {
                val errorMark = it.location.mark(fileContent.value, it.message)
                errorMark + it.hints.joinToString("\t----\n") {
                    val hintMark = it.location.mark(fileContent.value, it.message).replace(Regex("^", RegexOption.MULTILINE), "\t| ")
                    hintMark
                }
            })
            if (errors.isNotEmpty()) throw IllegalStateException()
        }
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