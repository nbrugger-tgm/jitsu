package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage

sealed interface ReasonedBoolean {
    fun and(boolean: ReasonedBoolean): ReasonedBoolean {
        return if (this.value && boolean.value) True("Both are true", this, boolean)
        else if (!this.value) this
        else boolean
    }

    fun or(boolean: ReasonedBoolean): ReasonedBoolean {
        return if (this.value && boolean.value) True("Both are true", this, boolean)
        else if (this.value) this
        else if (boolean.value) boolean
        else False("Both are false", this, boolean)
    }

    fun fullMessageChain(): Pair<String, MutableList<CompilerMessage.Hint>> {
        var msg = message
        val hints = hints.toMutableList()
        fun appendCauses(causes: List<Pair<String?, ReasonedBoolean>>, indent: String = "") {
            causes.forEach {
                msg += "\n$indent  ${it.first ?: "Because"} : ${it.second.message}"
                appendCauses(it.second.causes, "$indent  ")
                hints += it.second.hints
            }
        }
        appendCauses(causes)
        return msg to hints
    }

    val value: Boolean
    val message: String
    val hints: List<CompilerMessage.Hint>
    val causes: List<Pair<String?, ReasonedBoolean>>

    data class True(
        override val message: String,
        override val hints: List<CompilerMessage.Hint> = listOf(),
        override val causes: List<Pair<String?, ReasonedBoolean>> = listOf()
    ) :
        ReasonedBoolean {
        constructor(
            message: String,
            causes: List<ReasonedBoolean> = listOf()
        ) : this(
            message,
            causes = causes.map { null to it }
        )

        constructor(message: String, vararg hints: CompilerMessage.Hint) : this(message, hints = hints.toList())
        constructor(message: String, vararg causes: ReasonedBoolean) : this(message, causes = causes.toList())

        override val value: Boolean get() = true
    }

    data class False(
        override val message: String,
        override val hints: List<CompilerMessage.Hint> = listOf(),
        override val causes: List<Pair<String?, ReasonedBoolean>> = listOf()
    ) : ReasonedBoolean {

        constructor(
            message: String,
            causes: List<ReasonedBoolean> = listOf()
        ) : this(
            message,
            causes = causes.map { null to it }
        )

        constructor(message: String, vararg hints: CompilerMessage.Hint) : this(message, hints = hints.toList())
        constructor(message: String, vararg causes: ReasonedBoolean) : this(message, causes = causes.toList())

        override val value: Boolean get() = false
    }
}

fun <K, V> Map<K, V>.merge(other: Map<K, V>, merge: (V, V) -> V): Map<K, V> {
    val mutable = this.toMutableMap()
    other.forEach { (k, v) -> mutable[k] = mutable[k]?.let { merge(it, v) } ?: v }
    return mutable;
}