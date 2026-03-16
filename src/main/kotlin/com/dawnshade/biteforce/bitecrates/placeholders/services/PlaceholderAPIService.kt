package com.dawnshade.biteforce.bitecrates.placeholders.services

import com.dawnshade.biteforce.bitecrates.core.BiteCrates
import com.dawnshade.biteforce.bitecrates.placeholders.IPlaceholderService
import com.dawnshade.biteforce.bitecrates.placeholders.PlayerPlaceholder
import com.dawnshade.biteforce.bitecrates.placeholders.ServerPlaceholder
import com.dawnshade.biteforce.bitecrates.util.Utils
import eu.pb4.placeholders.api.PlaceholderContext
import eu.pb4.placeholders.api.PlaceholderResult
import eu.pb4.placeholders.api.Placeholders
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

class PlaceholderAPIService : IPlaceholderService {
    init {
        Utils.printInfo("PlaceholderAPI mod found! Enabling placeholder integration...")
    }

    override fun parsePlaceholders(player: ServerPlayer, text: String): String {
        return Placeholders.parseText(Component.literal(text), PlaceholderContext.of(player)).string
    }

    override fun registerPlayer(placeholder: PlayerPlaceholder) {
        Placeholders.register(ResourceLocation.fromNamespaceAndPath(BiteCrates.MOD_ID, placeholder.id())) { ctx, arg ->
            val player = ctx.player ?: return@register PlaceholderResult.invalid("NO PLAYER")
            val result = placeholder.handle(player, arg?.split(":") ?: emptyList())
            return@register if (result.isSuccessful) {
                PlaceholderResult.value(BiteCrates.INSTANCE.adventure.toNative(result.result))
            } else {
                PlaceholderResult.invalid(PlainTextComponentSerializer.plainText().serialize(result.result))
            }
        }
    }

    override fun registerServer(placeholder: ServerPlaceholder) {
        Placeholders.register(ResourceLocation.fromNamespaceAndPath(BiteCrates.MOD_ID, placeholder.id())) { ctx, arg ->
            val result = placeholder.handle(arg?.split(":") ?: emptyList())
            return@register if (result.isSuccessful) {
                PlaceholderResult.value(BiteCrates.INSTANCE.adventure.toNative(result.result))
            } else {
                PlaceholderResult.invalid(PlainTextComponentSerializer.plainText().serialize(result.result))
            }
        }
    }

    override fun finalizeRegister() {

    }
}
