package com.dawnshade.biteforce.bitecrates.placeholders.services

import com.dawnshade.biteforce.bitecrates.core.BiteCrates
import com.dawnshade.biteforce.bitecrates.placeholders.IPlaceholderService
import com.dawnshade.biteforce.bitecrates.placeholders.PlayerPlaceholder
import com.dawnshade.biteforce.bitecrates.placeholders.ServerPlaceholder
import com.dawnshade.biteforce.bitecrates.util.Utils
import io.github.miniplaceholders.api.Expansion
import io.github.miniplaceholders.api.MiniPlaceholders
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.minecraft.server.level.ServerPlayer

class MiniPlaceholdersService : IPlaceholderService {
    private val builder = Expansion.builder(BiteCrates.MOD_ID)
    private val miniMessage = MiniMessage.builder()
        .tags(TagResolver.builder().build())
        .build()

    init {
        Utils.printInfo("MiniPlaceholders mod found! Enabling placeholder integration...")
    }

    override fun parsePlaceholders(player: ServerPlayer, text: String): String {
        val resolver = TagResolver.resolver(
            MiniPlaceholders.getGlobalPlaceholders(),
            MiniPlaceholders.getAudiencePlaceholders(player)
        )

        return BiteCrates.INSTANCE.adventure.toNative(
            miniMessage.deserialize(text, resolver)
        ).string
    }

    override fun registerPlayer(placeholder: PlayerPlaceholder) {
        builder.filter(ServerPlayer::class.java)
            .audiencePlaceholder(placeholder.id()) { audience, queue, _ ->
                val arguments: MutableList<String> = mutableListOf()
                while (queue.peek() != null) {
                    arguments.add(queue.pop().toString())
                }
                return@audiencePlaceholder Tag.inserting(placeholder.handle(audience as ServerPlayer, arguments).result)
            }
    }

    override fun registerServer(placeholder: ServerPlaceholder) {
        builder.globalPlaceholder(placeholder.id()) { queue, _ ->
            val arguments: MutableList<String> = mutableListOf()
            while (queue.peek() != null) {
                arguments.add(queue.pop().toString())
            }
            return@globalPlaceholder Tag.inserting(placeholder.handle(arguments).result)
        }
    }

    override fun finalizeRegister() {
        builder.build().register()
    }
}
