package eu.nitok.jitsu.compiler.cli.commands

import eu.nitok.jitsu.compiler.cli.Jitsu
import picocli.CommandLine
import picocli.CommandLine.*
@Command(
    name = "compile",
    description = ["processes sourcecode and builds an internal graph"]
)
class Compile {
    @ParentCommand
    lateinit var cli: Jitsu;
    @Spec
    lateinit var spec: Model.CommandSpec
}