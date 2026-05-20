package eu.nitok.jitsu.common

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

class PathSerializer : KSerializer<Path> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("java.Path", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Path) {
        encoder.encodeString(value.absolutePathString())
    }

    override fun deserialize(decoder: Decoder): Path {
        return Path(decoder.decodeString())
    }
}