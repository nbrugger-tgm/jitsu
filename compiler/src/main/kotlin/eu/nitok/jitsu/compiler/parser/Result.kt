package eu.nitok.jitsu.compiler.parser

import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage

sealed interface Result<T> {
    data class Success<T>(val value: T) : Result<T>
    data class Failure<T>(val message: CompilerMessage) : Result<T>
}