package eu.nitok.jitsu.compiler.graph

fun <K, V> Map<K, V>.merge(other: Map<K, V>, merge: (V, V) -> V): Map<K, V> {
    val mutable = this.toMutableMap()
    other.forEach { (k, v) -> mutable[k] = mutable[k]?.let { merge(it, v) } ?: v }
    return mutable;
}