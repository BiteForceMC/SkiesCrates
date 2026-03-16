package com.dawnshade.biteforce.bitecrates.util

import com.eduardomcb.discord.webhook.WebhookManager
import com.eduardomcb.discord.webhook.models.Embed
import com.eduardomcb.discord.webhook.models.Image
import net.minecraft.server.level.ServerPlayer

object WebhookUtils {
    fun sendKeyAlert(url: String, player: ServerPlayer, message: String) {
        WebhookManager()
            .setChannelUrl(url)
            .setEmbeds(
                arrayOf(
                    Embed()
                        .setTitle("Duplicate Key Alert")
                        .setColor(0xFF0000)
                        .setDescription(message)
                        .setThumbnail(Image("https://mc-heads.net/body/${player.stringUUID}"))
                )
            )
            .exec()
    }
}