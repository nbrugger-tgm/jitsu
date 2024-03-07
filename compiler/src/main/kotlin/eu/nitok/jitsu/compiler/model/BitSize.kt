package eu.nitok.jitsu.compiler.model

enum class BitSize(val bits: Int) {
    BIT_8(8),
    BIT_16(16),
    BIT_32(32),
    BIT_64(64),
    BIT_128(128),
    BIT_256(256);

    companion object {
        fun byBits(bits: Int): BitSize? {
            return entries.find { it.bits == bits }
        }
    }
}