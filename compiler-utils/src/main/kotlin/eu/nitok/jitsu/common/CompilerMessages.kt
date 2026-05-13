package eu.nitok.jitsu.common

import eu.nitok.jitsu.common.locating.HasLocation
import eu.nitok.jitsu.common.locating.Locatable

data class CompilerMessages(
    val warnings: MutableList<CompilerMessage> = mutableListOf(),
    val errors: MutableList<CompilerMessage> = mutableListOf()
) {
    fun warn(warning: CompilerMessage) = warnings.add(warning)
    fun warn(message: String, location: Locatable, vararg hints: CompilerMessage.Hint) =
        warn(CompilerMessage(message, location, *hints))
    fun warn(message: String, location: HasLocation, vararg hints: CompilerMessage.Hint) =
        warn(CompilerMessage(message, location, *hints))

    fun error(error: CompilerMessage) = errors.add(error)
    fun error(message: String, location: Locatable, vararg hints: CompilerMessage.Hint) =
        error(CompilerMessage(message, location, *hints))
    fun error(message: String, location: HasLocation, vararg hints: CompilerMessage.Hint) =
        error(CompilerMessage(message, location, *hints))

    fun error(boolean: ReasonedBoolean, location: Locatable) {
        val fullMesageChain = boolean.fullMessageChain()
        this.error(CompilerMessage(fullMesageChain.first, location, fullMesageChain.second))
    }
    fun error(boolean: ReasonedBoolean, location: HasLocation) = error(boolean, location.location)

    fun add(messages: CompilerMessages) {
        warnings.addAll(messages.warnings)
        errors.addAll(messages.errors)
    }
}