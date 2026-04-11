package eu.nitok.jitsu.compiler.bitcode

import eu.nitok.jitsu.compiler.graph.JitsuFile
import java.nio.file.Path

/**
 * A fully lowered function ready for backend consumption.
 * Contains no graph types - only low-level IR.
 */
data class LoweredFunction(
    val name: String,
    val parameters: List<LoweredParameter>,
    val returnType: LowLevelType?,
    val body: LoweredBody
)

data class LoweredParameter(
    val name: String,
    val type: LowLevelType
)

sealed interface LoweredBody {
    /** Function with implementation - has lowered instructions */
    data class Implementation(val instructions: List<LowLevelInstruction>) : LoweredBody

    /** Native function - no instructions, just a native target string */
    data class Native(val nativeTarget: String) : LoweredBody
}

/**
 * A fully lowered module (corresponds to one source file).
 * Contains all lowered functions, ready for backend transpilation.
 */
data class LoweredModule(
    val sourcePath: String,
    val functions: List<LoweredFunction>
)

/**
 * Extension to lower a JitsuFile directly.
 */
fun JitsuFile.lower(sourcePath: Path): LoweredModule {
    return ModuleLowering(this, sourcePath).lower()
}
