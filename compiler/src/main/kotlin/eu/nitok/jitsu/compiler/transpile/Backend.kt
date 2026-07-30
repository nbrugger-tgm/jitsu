package eu.nitok.jitsu.compiler.transpile

import eu.nitok.jitsu.compiler.bitcode.LoweredModule
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.createParentDirectories

interface Backend {
    fun transpile(modules: Collection<LoweredModule>, dir: Path): List<Path>
    fun compile(files: Collection<Path>, dir: Path): Path

    fun createOutputFile(resolve: Path): Path {
        val code = resolve.createParentDirectories()
        try {
            code.createFile()
        } catch (_: FileAlreadyExistsException) {
        }catch (_: java.nio.file.FileAlreadyExistsException){
        }
        return code
    }
}
