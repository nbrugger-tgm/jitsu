package eu.nitok.jitsu.compiler

fun <K, V> Map<K, V>.merge(other: Map<K, V>, merge: (V, V) -> V): Map<K, V> {
    val mutable = this.toMutableMap()
    other.forEach { (k, v) -> mutable[k] = mutable[k]?.let { merge(it, v) } ?: v }
    return mutable;
}

inline fun <K, V> merge(vararg maps: Map<K, V>, merge: (V, V) -> V): Map<K, V> {
    return merge(maps.asList(), merge)
}
inline fun <K, V> merge(maps: Iterable<Map<K, V>>, merge: (V, V) -> V?): Map<K, V> {
    val mutable = mutableMapOf<K, V>()
    maps.forEach {
        it.forEach { (k, v) ->
            val prev = mutable[k]
            if(prev != null) {
                merge(prev, v)?.let { mutable[k] = it }
            } else mutable[k] = v
        }
    }
    return mutable
}