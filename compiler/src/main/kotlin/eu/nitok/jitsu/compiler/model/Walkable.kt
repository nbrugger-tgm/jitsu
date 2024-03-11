package eu.nitok.jitsu.compiler.model

interface Walkable<T> {
    val children: List<T>
}
fun <T:Walkable<T>> T.sequence(): Sequence<T> {
    return object : Iterator<T> {
        val nodeQueue = ArrayDeque(listOf(this@sequence))
        override fun hasNext(): Boolean {
            return !nodeQueue.isEmpty()
        }

        override fun next(): T {
            val subNode = nodeQueue.removeFirst()
            nodeQueue += subNode.children
            return subNode
        }
    }.asSequence()
}
fun <T:Walkable<T>,R> T.flatMap(mapper: (T) -> Iterable<R>): List<R> {
    return this.sequence().flatMap(mapper).toList()
}

/**
 * Maps the tree into a new tree
 */
fun <T:Walkable<T>,R> T.mapTree(mapper: (T, Iterable<R>) -> Iterable<R>): Iterable<R> {
    val children = this.children.flatMap { it.mapTree(mapper) }
    return mapper(this, children)
}