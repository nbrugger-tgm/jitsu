package eu.nitok.jitsu.compiler.parser



data class Location(val line: Int, val column: Int, var file: String? = null) : Locatable {
    override fun toString(): String {
        return if (file != null) {
            "$file:${charRefereneString()}"
        } else {
            charRefereneString()
        }
    }

    public fun charRefereneString() = "$line:$column"
    fun rangeTo(location: Location): Range {
        return Range(this, location)
    }
}
