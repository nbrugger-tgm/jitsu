package helpers

class Cache<T>(val source: ()->T) {
    private var value: T? = null
    fun get(): T{
        if(value != null) return value!!
        value = source()
        return value!!
    }
    fun invalidate() {
        value = null
    }
}