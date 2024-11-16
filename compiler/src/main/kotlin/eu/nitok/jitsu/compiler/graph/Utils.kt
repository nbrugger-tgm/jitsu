package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage
import eu.nitok.jitsu.compiler.graph.ReasonedBoolean.False
import kotlin.collections.plusAssign

sealed interface ReasonedBoolean {
    fun and(boolean: ReasonedBoolean): ReasonedBoolean {
        return if (this.value && boolean.value) True("Both are true", this, boolean)
        else if(!this.value) this
        else boolean
    }

    fun or(boolean: ReasonedBoolean): ReasonedBoolean {
        return if (this.value && boolean.value) True("Both are true", this, boolean)
        else if(this.value) this
        else if(boolean.value) boolean
        else False("Both are false", this, boolean)
    }

    fun fullMesageChain(): Pair<String, MutableList<CompilerMessage.Hint>> {
        var msg = message
        val hints = hints.toMutableList()
        fun appendCauses(causes: List<ReasonedBoolean>, indent: String = "") {
            causes.forEach {
                msg += "\n$indent\tCaused by: ${it.message}"
                appendCauses(it.causes, "$indent\t")
                hints += it.hints
            }
        }
        appendCauses(causes)
        return msg to hints
    }

    val value: Boolean
    val message: String
    val hints: List<CompilerMessage.Hint>
    val causes: List<ReasonedBoolean>

    data class True(
        override val message: String,
        override val hints: List<CompilerMessage.Hint>,
        override val causes: List<ReasonedBoolean> = listOf()
    ) :
        ReasonedBoolean {
        constructor(message: String, vararg hints: CompilerMessage.Hint) : this(
            message,
            hints.toList()
        )

        constructor(message: String) : this(
            message,
            listOf<CompilerMessage.Hint>()
        )
        constructor(message: String, vararg causes: ReasonedBoolean) : this(message, listOf(), causes.toList())

        override val value: Boolean get() = true
    }

    data class False(
        override val message: String,
        override val hints: List<CompilerMessage.Hint>,
        override val causes: List<ReasonedBoolean> = listOf()
    ) :
        ReasonedBoolean {
        constructor(message: String, vararg hints: CompilerMessage.Hint) : this(
            message,
            hints.toList()
        )

        constructor(message: String) : this(
            message,
            listOf<CompilerMessage.Hint>()
        )

        constructor(message: String, vararg causes: ReasonedBoolean) : this(message, listOf(), causes.toList())

        override val value: Boolean get() = false
    }
}

fun <K, V> Map<K, V>.merge(other: Map<K, V>, merge: (V, V) -> V): Map<K, V> {
    val mutable = this.toMutableMap()
    other.forEach { (k, v) -> mutable[k] = mutable[k]?.let { merge(it, v) } ?: v }
    return mutable;
}