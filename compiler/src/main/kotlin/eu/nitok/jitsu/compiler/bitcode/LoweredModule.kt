package eu.nitok.jitsu.compiler.bitcode

import eu.nitok.jitsu.compiler.graph.JitsuModule
import java.nio.file.Path

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

data class LoweredModule(val name: String, val functions: List<LoweredFunction>)

fun JitsuModule.lower(): LoweredModule {
    return ModuleLowering(this).lower()
}
