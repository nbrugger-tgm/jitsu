package eu.nitok.jitsu.common.locating

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.net.URI

@Serializable(with = Locatable.Serializer::class)
sealed interface Locatable {
    fun format(): String;

    /**
     * Includes the file name
     */
    fun absoluteFormat(): String;

    /**
     * Formats the location as file:line:column
     */
    fun absolutePositionFormat(): String;

    /**
     * Marks the location in the text adding an optional note to it
     */
    fun mark(note: String): String;
    fun toLocation(): Location
    val file: URI;

    object Serializer : KSerializer<Locatable> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("jitsu.Location", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Locatable) {
            val prefix = when (value) {
                is Location -> "L"
                is Position -> "P"
            }
            encoder.encodeString(prefix + value.absoluteFormat())
        }

        override fun deserialize(decoder: Decoder): Locatable {
            val string = decoder.decodeString()
            return when (string.first()) {
                'L' -> Location.Serializer.deserialize(string.drop(1))
                'P' -> Position.Serializer.deserialize(string.drop(1))
                else -> throw SerializationException("Invalid location format (no P or L prefix): $string")
            }
        }
    }
}
