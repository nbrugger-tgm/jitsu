package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage

sealed interface ReasonedBoolean {
    data object True: ReasonedBoolean
    data class False(val message: String, val hints: List<CompilerMessage.Hint>) : ReasonedBoolean{
        constructor(message: String, vararg hints: CompilerMessage.Hint) : this(
            message,
            hints.toList()
        )
    }
}

fun <K,V> Map<K,V>.merge(other: Map<K,V>, merge: (V,V)->V): Map<K,V> {
    val mutable = this.toMutableMap()
    other.forEach { (k, v) -> mutable[k] = mutable[k]?.let { merge(it, v) }?: v }
    return mutable;
}