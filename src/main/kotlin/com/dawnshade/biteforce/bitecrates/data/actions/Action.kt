package com.dawnshade.biteforce.bitecrates.data.actions

import com.google.gson.*
import com.dawnshade.biteforce.bitecrates.data.actions.ActionType.Companion.valueOfAnyCase
import eu.pb4.sgui.api.gui.SimpleGui
import net.minecraft.server.level.ServerPlayer
import java.lang.reflect.Type

abstract class Action(
    val type: ActionType
) {
    abstract fun executeAction(player: ServerPlayer, gui: SimpleGui)

    override fun toString(): String {
        return "Action(type=$type)"
    }

    internal class Adapter : JsonSerializer<Action>, JsonDeserializer<Action> {
        override fun serialize(src: Action, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            return context.serialize(src, src::class.java)
        }

        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Action {
            val jsonObject: JsonObject = json.getAsJsonObject()
            val value = jsonObject.get("type").asString
            val type: ActionType? = valueOfAnyCase(value)
            return try {
                context.deserialize(json, type!!.clazz)
            } catch (e: NullPointerException) {
                throw JsonParseException("Could not deserialize action type: $value", e)
            }
        }
    }
}
