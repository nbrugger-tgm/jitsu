package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.graph.elements.AccessibleElement

internal class IrStore<T: AccessibleElement> {
    private val db: MutableMap<Int, T> = mutableMapOf()
    private var nextId = 0;
    fun getSymbolId(func: T): Int {
        val existingID = func.symbolIndex
        if(existingID == null) {
            val newId = nextId++
            db[newId] = func
            func.symbolIndex = newId
            return newId
        }
        db.putIfAbsent(existingID, func)
        return existingID
    }

    operator fun get(id: Int): T {
        return db[id]!!
    }
}