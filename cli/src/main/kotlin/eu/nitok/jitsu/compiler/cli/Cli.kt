package eu.nitok.jitsu.compiler.cli

import eu.nitok.jitsu.compiler.cli.commands.Compile
import eu.nitok.jitsu.compiler.cli.commands.Parse
import eu.nitok.jitsu.compiler.cli.commands.Process
import eu.nitok.jitsu.compiler.cli.commands.Transpile
import picocli.CommandLine
import picocli.CommandLine.*
import picocli.CommandLine.Model.CommandSpec
import java.io.File
import java.nio.file.Paths
import kotlin.system.exitProcess


@Command(
    name="jitsu",
    scope = ScopeType.INHERIT,
    synopsisSubcommandLabel = "COMMAND", subcommands = [
        Parse::class, Compile::class, Process::class,
        Transpile::class
    ]
)
class Jitsu : Runnable {
    @Spec
    lateinit var spec: CommandSpec
    override fun run() {
        throw ParameterException(spec.commandLine(), "Missing required subcommand")
    }
}

fun main(args: Array<String>): Unit = exitProcess(CommandLine(Jitsu()).execute(*args))