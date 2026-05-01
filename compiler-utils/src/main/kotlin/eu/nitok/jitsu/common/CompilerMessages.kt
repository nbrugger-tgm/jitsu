package eu.nitok.jitsu.common

import kotlinx.serialization.Serializable

@Serializable
data class CompilerMessages(
    val warnings: MutableList<CompilerMessage> = mutableListOf(),
    val errors: MutableList<CompilerMessage> = mutableListOf()
) {
    fun warn(warning: CompilerMessage) = warnings.add(warning)
    fun warn(message: String, location: Range, vararg hints: CompilerMessage.Hint) =
        warnings.add(CompilerMessage(message, location, hints.toList()))

    fun error(error: CompilerMessage) = errors.add(error)
    fun error(message: String, location: Range, vararg hints: CompilerMessage.Hint) =
        errors.add(CompilerMessage(message, location, hints.toList()))

    fun error(message: String, located: Located<*>, vararg hints: CompilerMessage.Hint) =
        error(message, located.location, *hints)

    fun error(boolean: ReasonedBoolean, location: Range) {
        val fullMesageChain = boolean.fullMessageChain()
        this.error(CompilerMessage(fullMesageChain.first, location, fullMesageChain.second))
    }

    fun add(messages: CompilerMessages) {
        messages.warnings.addAll(warnings)
        messages.errors.addAll(errors)
    }
}