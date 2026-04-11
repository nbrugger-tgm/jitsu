package eu.nitok.jitsu.compiler.transpile

import eu.nitok.jitsu.compiler.bitcode.LoweredModule
import java.nio.file.Path

interface Backend {
    fun transpile(modules: Collection<LoweredModule>, dir: Path): List<Path>
    fun compile(files: Collection<Path>, dir: Path): Path
}
