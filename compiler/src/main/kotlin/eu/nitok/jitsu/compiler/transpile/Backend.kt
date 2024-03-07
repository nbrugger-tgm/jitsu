package eu.nitok.jitsu.compiler.transpile

import eu.nitok.jitsu.compiler.graph.Scope
import java.nio.file.Path

interface Backend {
    fun transpile(graphs: Collection<Scope>, dir: Path): List<Path>
    fun compile(files: Collection<Path>, dir: Path): Path
}