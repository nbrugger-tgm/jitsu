package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage
import eu.nitok.jitsu.compiler.parser.Range
import kotlinx.serialization.Serializable
import java.io.Reader

sealed interface ReasonedBoolean {
    data object True: ReasonedBoolean
    data class False(val message: String, val hints: List<CompilerMessage.Hint>) : ReasonedBoolean{
        constructor(message: String, vararg hints: CompilerMessage.Hint) : this(
            message,
            hints.toList()
        )
    }
}


sealed interface Reference {
    val referenced: String
}