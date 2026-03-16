package com.dawnshade.biteforce.bitecrates.util

import com.google.gson.*
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import com.dawnshade.biteforce.bitecrates.core.BiteCrates
import com.dawnshade.biteforce.bitecrates.config.ConfigManager
import net.minecraft.core.BlockPos
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.chunk.LevelChunk
import java.lang.reflect.Type

object Utils {
    
    fun printDebug(message: String?, bypassCheck: Boolean = false) {
        if (bypassCheck || ConfigManager.CONFIG.debug)
            BiteCrates.LOGGER.info("[${BiteCrates.MOD_NAME}] DEBUG: $message")
    }

    fun printError(message: String?) {
        BiteCrates.LOGGER.error("[${BiteCrates.MOD_NAME}] ERROR: $message")
    }

    fun printInfo(message: String?) {
        BiteCrates.LOGGER.info("[${BiteCrates.MOD_NAME}] $message")
    }

    fun getLevel(name: String): ServerLevel? {
        val id = ResourceLocation.tryParse(name) ?: return null
        return BiteCrates.INSTANCE.server
            .levelKeys()
            .firstOrNull { it.location() == id }
            ?.let { BiteCrates.INSTANCE.server.getLevel(it) }
    }

    
    fun getFormattedTime(time: Long): String {
        if (time <= 0) return "0s"
        val timeFormatted: MutableList<String> = ArrayList()
        val days = time / 86400
        val hours = time % 86400 / 3600
        val minutes = time % 86400 % 3600 / 60
        val seconds = time % 86400 % 3600 % 60
        if (days > 0) {
            timeFormatted.add(days.toString() + "d")
        }
        if (hours > 0) {
            timeFormatted.add(hours.toString() + "h")
        }
        if (minutes > 0) {
            timeFormatted.add(minutes.toString() + "m")
        }
        if (seconds > 0) {
            timeFormatted.add(seconds.toString() + "s")
        }
        return java.lang.String.join(" ", timeFormatted)
    }

    
    data class RegistrySerializer<T>(val registry: Registry<T>) : JsonSerializer<T>, JsonDeserializer<T> {
        @Throws(JsonParseException::class)
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): T? {
            val parsed = if (json.isJsonPrimitive) registry.get(ResourceLocation.tryParse(json.asString)) else null
            if (parsed == null)
                printError("There was an error while deserializing a Registry Type: $registry")
            return parsed
        }
        override fun serialize(src: T, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            return JsonPrimitive(registry.getId(src).toString())
        }
    }

    data class CodecSerializer<T>(val codec: Codec<T>) : JsonSerializer<T>, JsonDeserializer<T> {
        @Throws(JsonParseException::class)
        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): T? {
            return codec.decode(JsonOps.INSTANCE, json).orThrow.first
        }

        override fun serialize(src: T?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            return codec.encodeStart(JsonOps.INSTANCE, src).orThrow
        }
    }
}

fun LevelChunk.contains(pos: BlockPos): Boolean {
    val chunkX = pos.x shr 4
    val chunkZ = pos.z shr 4
    return this.pos.x == chunkX && this.pos.z == chunkZ
}
