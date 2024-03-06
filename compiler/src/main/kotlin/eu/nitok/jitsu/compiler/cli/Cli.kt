package eu.nitok.jitsu.compiler.cli

import eu.nitok.jitsu.compiler.cli.commands.Transpile
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Model.CommandSpec
import picocli.CommandLine.ParameterException
import picocli.CommandLine.Spec
import kotlin.system.exitProcess


@Command(synopsisSubcommandLabel = "COMMAND", subcommands = [
    Transpile::class
])
data object Jitsu: Runnable {
    @Spec lateinit var spec: CommandSpec
    override fun run() {
        throw ParameterException(spec.commandLine(), "Missing required subcommand")
    }
}

fun main(args: Array<String>) : Unit = exitProcess(CommandLine(Jitsu).execute(*args))