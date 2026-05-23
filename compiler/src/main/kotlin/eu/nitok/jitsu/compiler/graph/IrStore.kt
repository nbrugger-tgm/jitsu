package eu.nitok.jitsu.compiler.graph

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.IdentityHashMap

@Serializable
internal class IrStore<T> {
    @Transient private val funcIds = IdentityHashMap<T, Int>()
    @Transient private var nextFuncId = 0;
    private val db: MutableList<T> = mutableListOf()
    fun getSymbolId(func: T): Int {
        return funcIds.getOrPut(func) {
            db.add(func)
            nextFuncId++
        }
    }
    operator fun get(id: Int): T {
        return db[id]
    }
}