package helpers

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors

val VIRTUAL_THREADS = Executors.newVirtualThreadPerTaskExecutor()

fun <T> Collection<CompletableFuture<out T>>.joinAll(): CompletableFuture<out List<T>> {
    if(this.isEmpty()) return CompletableFuture.completedFuture(emptyList())
    if(this.size == 1) return this.iterator().next().thenApply { listOf(it) }
    return asSequence().fold(CompletableFuture.completedFuture<MutableList<T>>(mutableListOf())) { a, b ->
        a.thenCombine(b) { list, value ->
            list.add(value)
            list
        }
    }
}