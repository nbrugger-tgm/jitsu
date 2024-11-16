package eu.nitok.jitsu.compiler.model

interface Walkable<T> {
    val children: List<T>
}

/**
 * An iterator that iterates through all nodes using breath first
 */
fun <T : Walkable<T>> T.sequence(): Sequence<T> = object : Iterator<T> {
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

/**
 * Walk down the tree using breath first allowing to stop walking branches
 *
 * @return true if the walk should continue, false if the walk at this branch should stop
 */
fun <T : Walkable<T>> T.walk(predicate: (T) -> Boolean) {
    val nodeQueue = ArrayDeque(listOf(this))
    while (!nodeQueue.isEmpty()) {
        val subNode = nodeQueue.removeFirst()
        val continueDescend = predicate(subNode)
        if (continueDescend) nodeQueue += subNode.children
    }
}

fun <T : Walkable<T>, R> T.flatMap(mapper: (T) -> Iterable<R>): List<R> {
    return this.sequence().flatMap(mapper).toList()
}

/**
 * Maps the tree into a new tree
 */
fun <T : Walkable<T>, R> T.mapTree(mapper: (T, Iterable<R>) -> Iterable<R>): Iterable<R> {
    val children = this.children.flatMap { it.mapTree(mapper) }
    return mapper(this, children)
}