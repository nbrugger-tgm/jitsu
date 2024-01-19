package eu.nitok.jitsu.compiler.diagnostic

import eu.nitok.jitsu.compiler.ast.Location
import kotlinx.serialization.Serializable

@Serializable
class CompilerError(
    var knownError: ErrorCode,
    var message: String,
    val location: Location,
    val hints: List<Hint> = emptyList()
) {
    @Serializable
    data class Hint(val message: String, val location: Location)

    @Serializable
    enum class ErrorCode(val stage: Stage, val id: Int) {
        ;

        override fun toString(): String {
            return stage.code + id
        }

        enum class Stage(val code: String) {
            TOKENIZING("TK"),
            PARSING("PS"),
            PREPROCESS("PP"),
            LINKING("LN"),
            COMPILER_PLUGINS("CP"),
            ANALYSIS("AN"),
            IR_GENERATION("IG"),
            LOWERING("LO"),
            EXTERNAL("EX"),
            ASSEMBLING("ASM")
        }
    }
}