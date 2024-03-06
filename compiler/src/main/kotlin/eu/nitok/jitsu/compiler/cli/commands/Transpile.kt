package eu.nitok.jitsu.compiler.cli.commands

import picocli.CommandLine
@CommandLine.Command(
    name = "transpile",
    description = ["generates code for a target language from jitsu"]
)
class Transpile {
    @CommandLine.Option(names = ["-b", "--backend"], description = ["The backend to compile to (llvm, rust, c, js)"])
    var backendName = "rust"
}