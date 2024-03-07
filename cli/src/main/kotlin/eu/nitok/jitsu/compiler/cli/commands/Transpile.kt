package eu.nitok.jitsu.compiler.cli.commands

import eu.nitok.jitsu.compiler.cli.BackendRegistry
import picocli.CommandLine
import picocli.CommandLine.Mixin
import java.io.File
import java.nio.file.Path
import java.util.concurrent.Callable

@CommandLine.Command(
    name = "transpile",
    description = ["generates code for a target language from jitsu"]
)
class Transpile: Callable<List<Path>> {
    @Mixin
    lateinit var cli: Process
    @CommandLine.Option(names = ["-b", "--backend"], description = ["The backend to compile to (llvm, rust, c, js)"])
    var backendName = "rust"
    override fun call(): List<Path> {
        val graphs = cli.call()
        val backend = BackendRegistry.create(backendName)
        val files = backend.run {
            transpile(graphs, cli.cli.outpurDir.resolve(backendName))
        }
        cli.spec.commandLine().out.println("Transpiled to $files")
        return files
    }
}