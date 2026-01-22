package eu.nitok.jitsu.compiler.bitcode

const val REF_SIZE = 64

sealed interface MemoryFragment {
    val size: Long
    data class Reference(val referencedLayout: MemoryFragment) : MemoryFragment {
        override val size: Long = REF_SIZE.toLong()
    }
    data class Value(
        override val size: Long
    ) : MemoryFragment
}

data class MemoryLayout(
    val segements: List<Segment>
): MemoryFragment {
    constructor(vararg segments: Segment) : this(segments.toList())
    override val size = segements.map { it.layout.size }.reduce(Long::plus)
    data class Segment(
        val name: String?,
        val layout: MemoryFragment,
    ) {
        val size: Long get() = layout.size
    }
}