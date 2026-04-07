package eu.nitok.jitsu.compiler.bitcode

import eu.nitok.jitsu.compiler.graph.Type
import eu.nitok.jitsu.compiler.graph.TypeDefinition

const val STACK_ALLOC_LIMIT = 128

val Type.layout : MemoryFragment get() = when(this) {
    is Type.Primitive -> MemoryFragment.Value(size.bits.toLong())
    Type.Null -> MemoryFragment.Value(0)
    Type.Undefined -> MemoryFragment.Reference(MemoryFragment.Value(0))
    is Type.TypeReference -> this.resolvedCache.layout
    is Type.Union -> layout()
    is Type.StructuralInterface -> layout()
    is Type.Array -> layout()
    is TypeDefinition.DirectTypeDefinition.Enum -> MemoryFragment.Value(leastBitsToHold(this.constants.size.toLong()).toLong())
    is Type.FunctionTypeSignature -> TODO()
    is Type.Value -> TODO()
}

private fun Type.Array.layout(): MemoryFragment = if(size == null || !size.isConstant.value) MemoryLayout(
    MemoryLayout.Segment("size", MemoryFragment.Value(64)),
    MemoryLayout.Segment("content", MemoryFragment.Reference(MemoryFragment.Value(0L)))
) else {
    //constant calc needed!
    // MemoryLayout(List(constantValue(size).value.toInt()) { MemoryLayout.Segment(null, elementType.layout) })
    TODO()
}
private fun Type.StructuralInterface.layout(): MemoryLayout = MemoryLayout(
    fields.map { MemoryLayout.Segment(it.key, it.value.type.layout) }
)

private fun Type.Union.layout(): MemoryLayout = MemoryLayout(
    listOf(
        MemoryLayout.Segment("type", MemoryFragment.Value(leastBitsToHold(this.options.size.toLong()).toLong())),
        MemoryLayout.Segment("value", MemoryFragment.Value(this.options.map { it.layout }.maxBy { it.size }.size))
    )
)

fun powInt(base: Int, exp: Int): Long {
    var result = 1L
    repeat(exp) {
        result *= base
    }
    return result
}
fun leastBitsToHold(value: Long): Int {
    var bits = 0
    var currentMax = 1L
    while(currentMax <= value) {
        currentMax = currentMax shl 1
        bits++
    }
    return bits
}