package com.dawnshade.biteforce.bitecrates.util

import com.dawnshade.biteforce.bitecrates.core.BiteCrates
import com.dawnshade.biteforce.bitecrates.placeholders.PlaceholderManager
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

object TextUtils {
    fun toNative(text: String): Component {
        return BiteCrates.INSTANCE.adventure.toNative(toComponent(text))
    }

    fun toComponent(text: String): net.kyori.adventure.text.Component {
        return BiteCrates.MINI_MESSAGE.deserialize(text)
    }

    fun parseAllNative(player: ServerPlayer, text: String, additionalPlaceholders: Map<String, String> = emptyMap()): Component {
        return toNative(
            PlaceholderManager.parse(player, text, additionalPlaceholders)
        )
    }
}
