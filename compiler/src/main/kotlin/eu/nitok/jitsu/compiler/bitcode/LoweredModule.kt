package eu.nitok.jitsu.compiler.bitcode

import eu.nitok.jitsu.compiler.graph.api.JitsuModule

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
    object Native : LoweredBody
}

data class LoweredModule(val name: String, val functions: List<LoweredFunction>)

fun JitsuModule.lower(): LoweredModule {
    //TODO: lower to list to have nice names
    return ModuleLowering(this).lower()
}
