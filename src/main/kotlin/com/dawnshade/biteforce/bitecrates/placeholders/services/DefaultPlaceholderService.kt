package com.dawnshade.biteforce.bitecrates.placeholders.services

import com.dawnshade.biteforce.bitecrates.placeholders.IPlaceholderService
import com.dawnshade.biteforce.bitecrates.placeholders.PlayerPlaceholder
import com.dawnshade.biteforce.bitecrates.placeholders.ServerPlaceholder
import net.minecraft.server.level.ServerPlayer

class DefaultPlaceholderService : IPlaceholderService {
    override fun parsePlaceholders(player: ServerPlayer, text: String): String {
        return text
            .replace("%player%", player.name.string)
            .replace("%player_uuid%", player.uuid.toString())
    }

    override fun registerPlayer(placeholder: PlayerPlaceholder) {

    }

    override fun registerServer(placeholder: ServerPlaceholder) {

    }

    override fun finalizeRegister() {

    }
}
