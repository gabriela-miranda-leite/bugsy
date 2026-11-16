package com.bugsy.core.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull

@Serializable
data class DebugFlag(
    val key: String,
    val label: String,
    @Serializable(with = FlagValueSerializer::class) val value: Any,
    val isOverridden: Boolean
)

/**
 * [DebugFlag.value] is `Any` (a `Boolean`, `String` or number set by the host app's `flagsProvider`), which
 * kotlinx.serialization can't serialize generically. Only usable with [kotlinx.serialization.json.Json] (it's
 * for the BugsyWsServer protocol, see DEB-58), since it round-trips through [JsonElement] directly.
 */
internal object FlagValueSerializer : KSerializer<Any> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Any) {
        val jsonEncoder = encoder as? JsonEncoder ?: error("FlagValueSerializer only works with Json")
        val element = when (value) {
            is Boolean -> JsonPrimitive(value)
            is Int -> JsonPrimitive(value)
            is Long -> JsonPrimitive(value)
            is Double -> JsonPrimitive(value)
            is String -> JsonPrimitive(value)
            else -> JsonPrimitive(value.toString())
        }
        jsonEncoder.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): Any {
        val jsonDecoder = decoder as? JsonDecoder ?: error("FlagValueSerializer only works with Json")
        val primitive = jsonDecoder.decodeJsonElement() as? JsonPrimitive ?: return ""
        return when {
            primitive.isString -> primitive.content
            primitive.booleanOrNull != null -> primitive.boolean
            primitive.longOrNull != null -> primitive.long
            primitive.doubleOrNull != null -> primitive.double
            else -> primitive.content
        }
    }
}
