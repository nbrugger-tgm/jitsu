package eu.nitok.jitsu.compiler.graph

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(SymbolIDSerializer::class)
data class SymbolID(val module: String?, val index: Int)

internal object SymbolIDSerializer : KSerializer<SymbolID> {
    // Serial names of descriptors should be unique, this is why we advise including app package in the name.
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("sid", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: SymbolID) {
        if(value.module != null) encoder.encodeString("${value.module}/${value.index.toString(36)}")
        else encoder.encodeString(value.index.toString(36))
    }

    override fun deserialize(decoder: Decoder): SymbolID {
        val string = decoder.decodeString()
        val split = string.indexOf('/')
        return if(split != -1) SymbolID(string.substring(0, split), string.substring(split+1).toInt(36))
        else SymbolID(null, string.toInt(36))
    }
}