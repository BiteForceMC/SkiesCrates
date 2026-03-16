package com.dawnshade.biteforce.bitecrates.config

import com.google.gson.*
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import java.lang.reflect.Type

class SoundOption(
    val sound: String,
    val volume: Float,
    val pitch: Float,
    val source: String?,
) {
    fun playSound(player: ServerPlayer) {
        var category = if (source == null) SoundSource.MASTER else SoundSource.entries.firstOrNull { it.name.equals(source, true) }
        if (category == null) {
            category = SoundSource.MASTER
        }

        player.playNotifySound(
            SoundEvent.createVariableRangeEvent(ResourceLocation.parse(sound)),
            category,
            volume,
            pitch,
        )
    }

    class Adaptor : JsonDeserializer<SoundOption>, JsonSerializer<SoundOption> {
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): SoundOption {
            return when (json) {
                is JsonPrimitive -> {
                    
                    SoundOption(json.asString, 1.0f, 1.0f, null)
                }
                is JsonObject -> {
                    
                    val sound = json.get("sound").asString
                    val volume = json.get("volume")?.asFloat ?: 1.0f
                    val pitch = json.get("pitch")?.asFloat ?: 1.0f
                    val source = json.get("source")?.asString
                    SoundOption(sound, volume, pitch, source)
                }
                else -> throw JsonParseException("Invalid sound format")
            }
        }

        override fun serialize(src: SoundOption, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            val obj = JsonObject()
            obj.addProperty("sound", src.sound)
            obj.addProperty("volume", src.volume)
            obj.addProperty("pitch", src.pitch)
            if (src.source != null) {
                obj.addProperty("source", src.source)
            }
            return obj
        }
    }
}
