package eu.nitok.jitsu.compiler.cli.commands

import eu.nitok.jitsu.compiler.parser.parseFile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import picocli.CommandLine.*
import picocli.CommandLine.Model.CommandSpec
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.Callable
import kotlin.io.path.*


@Command(
    name = "transpile",
    description = ["transpiles sourcecode into another language using a specified backend"]
)
class Transpile : Callable<Int> {
    @Spec lateinit var spec: CommandSpec
    @Parameters(description = ["All source-files that are part of the transpilation"])
    var classpath: List<File> = listOf()

    @Option(names = ["-b", "--backend"], description = ["The backend to compile to (llvm, rust, c, js)"])
    var backendName = "rust"

    @Option(names = ["-o", "--output"])
    var outpurDir = Paths.get("build")

    override fun call(): Int {
        spec.commandLine().out.println("Compile : $classpath")
        var asts = classpath.map {
            parseFile(it.bufferedReader(bufferSize = 100*40), it.toURI()) to it
        }
        val cacheDirectory = outpurDir.resolve("cache")
        if(!cacheDirectory.exists()) cacheDirectory.createParentDirectories().createDirectory()
        val parsedCache = cacheDirectory.resolve("ast")
        if(!parsedCache.exists()) parsedCache.createDirectory()
        asts.forEach {
            val cache = parsedCache.resolve("${it.second.nameWithoutExtension}.ast.json");
            if(!cache.exists()) cache.createFile()
            cache.writeText(Json.encodeToString(it.first))
            spec.commandLine().out.println("Write ast cache to ${cache.absolutePathString()}")
        }
        var errors = asts.map {
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
                    val hintMark = it.location.mark(fileContent.value, it.message).replace("\n","\n\t|")
                    spec.commandLine().err.println(hintMark)
                }
            }
            if(it.first.isNotEmpty()) return 1
        }
        return 0;
    }
}