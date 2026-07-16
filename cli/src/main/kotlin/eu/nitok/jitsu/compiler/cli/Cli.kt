package eu.nitok.jitsu.compiler.cli

import picocli.CommandLine
import picocli.CommandLine.*
import picocli.CommandLine.Model.CommandSpec
import kotlin.system.exitProcess


@Command(
    name="jitsu",
    scope = ScopeType.INHERIT,
    synopsisSubcommandLabel = "COMMAND", subcommands = []
)
class Jitsu : Runnable {
    @Spec
    lateinit var spec: CommandSpec
    override fun run() {
        throw ParameterException(spec.commandLine(), "Missing required subcommand")
    }
}

fun main(args: Array<String>): Unit = exitProcess(CommandLine(Jitsu()).execute(*args))