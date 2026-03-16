package com.dawnshade.biteforce.bitecrates.placeholders.services

import com.dawnshade.biteforce.bitecrates.core.BiteCrates
import com.dawnshade.biteforce.bitecrates.placeholders.IPlaceholderService
import com.dawnshade.biteforce.bitecrates.placeholders.PlayerPlaceholder
import com.dawnshade.biteforce.bitecrates.placeholders.ServerPlaceholder
import com.dawnshade.biteforce.bitecrates.util.Utils
import net.impactdev.impactor.api.platform.players.PlatformPlayer
import net.impactdev.impactor.api.platform.sources.PlatformSource
import net.impactdev.impactor.api.text.TextProcessor
import net.impactdev.impactor.api.utility.Context
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.minecraft.server.level.ServerPlayer


class ImpactorPlaceholderService : IPlaceholderService {
    private val processor = TextProcessor.mini(
        MiniMessage.builder()
            .tags(TagResolver.builder().build())
            .build()
    )

    init {
        Utils.printInfo("Impactor mod found! Enabling placeholder integration...")
    }

    override fun parsePlaceholders(player: ServerPlayer, text: String): String {
        val platformPlayer = PlatformPlayer.getOrCreate(player.uuid)
        return BiteCrates.INSTANCE.adventure.toNative(
            processor.parse(platformPlayer, text, Context().append(PlatformSource::class.java, platformPlayer))
        ).string
    }

    override fun registerPlayer(placeholder: PlayerPlaceholder) {

    }

    override fun registerServer(placeholder: ServerPlaceholder) {

    }

    override fun finalizeRegister() {

    }
}
