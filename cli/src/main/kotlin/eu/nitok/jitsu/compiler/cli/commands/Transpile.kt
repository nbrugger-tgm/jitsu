package eu.nitok.jitsu.compiler.cli.commands

import picocli.CommandLine
import picocli.CommandLine.Mixin
import java.io.File
import java.util.concurrent.Callable

@CommandLine.Command(
    name = "transpile",
    description = ["generates code for a target language from jitsu"]
)
class Transpile: Callable<List<File>> {
    @Mixin
    lateinit var cli: Process
    @CommandLine.Option(names = ["-b", "--backend"], description = ["The backend to compile to (llvm, rust, c, js)"])
    var backendName = "rust"
    override fun call(): List<File> {
        val graphs = cli.call()
        return graphs.map { graph ->
            val backend = BackendRegistry.create(backendName)
            backend.transpile(graph)
        }
    }
}