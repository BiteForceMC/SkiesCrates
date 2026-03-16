package com.dawnshade.biteforce.bitecrates.feature.opening.world

import com.google.gson.*
import com.dawnshade.biteforce.bitecrates.feature.opening.OpeningAnimation
import java.lang.reflect.Type

abstract class WorldOpeningAnimation(
    val type: WorldAnimationType
): OpeningAnimation {
    abstract fun setup(opening: WorldOpeningInstance)
    abstract fun tick(opening: WorldOpeningInstance)
    abstract fun stop(opening: WorldOpeningInstance)

    internal class Adapter : JsonSerializer<WorldOpeningAnimation>, JsonDeserializer<WorldOpeningAnimation> {
        override fun serialize(src: WorldOpeningAnimation, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            return context.serialize(src, src::class.java)
        }

        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): WorldOpeningAnimation {
            val jsonObject: JsonObject = json.getAsJsonObject()
            val value = jsonObject.get("type").asString
            val type: WorldAnimationType? = WorldAnimationType.valueOfAnyCase(value)
            return try {
                context.deserialize(json, type!!.clazz)
            } catch (e: Exception) {
                throw JsonParseException("Could not deserialize world animation type: $value", e)
            }
        }
    }
}