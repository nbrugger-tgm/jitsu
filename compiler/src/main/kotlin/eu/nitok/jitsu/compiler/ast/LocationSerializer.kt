package eu.nitok.jitsu.compiler.ast

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

object LocationSerializer : KSerializer<Location> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Location") {
        element<Int>("fromLine")
        element<Int>("toLine")
        element<Int>("fromColumn")
        element<Int>("toColumn")
    }
    override fun serialize(encoder: Encoder, value: Location) = encoder.encodeStructure(descriptor) {
        encodeIntElement(descriptor, 0, value.fromLine)
        encodeIntElement(descriptor, 1, value.toLine)
        encodeIntElement(descriptor, 2, value.fromColumn)
        encodeIntElement(descriptor, 3, value.toColumn)
    }
    override fun deserialize(decoder: Decoder): Location = decoder.decodeStructure(descriptor) {
        Location.of(
            decodeIntElement(descriptor, 0),
            decodeIntElement(descriptor, 2),
            decodeIntElement(descriptor, 1),
            decodeIntElement(descriptor, 3)
        );
    }
}