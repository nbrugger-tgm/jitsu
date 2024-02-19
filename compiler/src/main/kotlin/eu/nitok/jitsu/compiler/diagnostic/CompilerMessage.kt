package eu.nitok.jitsu.compiler.diagnostic


import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage.ErrorCode.Stage.PARSING
import eu.nitok.jitsu.compiler.parser.Locatable
import kotlinx.serialization.Serializable

@Serializable
class CompilerMessage(
    var knownError: ErrorCode,
    var message: String,
    val location: Locatable,
    val hints: List<Hint> = emptyList()
) {
    @Serializable
    data class Hint(val message: String, val location: Locatable)

    @Serializable
    enum class ErrorCode(private val stage: Stage, private val id: Int) {
        EXPECT_EOF(PARSING, 1),
        EXPECT_STATEMENT(PARSING,2),
        IDENTIFIER_START_LETTER(PARSING, 3),
        IDENTIFIER_ILLEGAL_CHARACTER(PARSING, 4),
        IDENTIFIER_CONTAINS_DOLLAR(PARSING, 5),;

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