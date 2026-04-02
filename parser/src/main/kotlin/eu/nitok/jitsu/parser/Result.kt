package eu.nitok.jitsu.parser

import eu.nitok.jitsu.common.CompilerMessage
import eu.nitok.jitsu.common.Range

sealed interface Result<T> {
    data class Success<T>(val value: T) : Result<T>
    data class Failure<T>(val message: CompilerMessage) : Result<T> {
        constructor(message: String, location: Range, hints: List<CompilerMessage.Hint> = emptyList()) : this(
            CompilerMessage(message, location, hints)
        )
    }
}
