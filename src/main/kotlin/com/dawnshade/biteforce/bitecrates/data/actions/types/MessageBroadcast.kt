package com.dawnshade.biteforce.bitecrates.data.actions.types

import com.google.gson.annotations.JsonAdapter
import com.dawnshade.biteforce.bitecrates.core.BiteCrates
import com.dawnshade.biteforce.bitecrates.data.actions.Action
import com.dawnshade.biteforce.bitecrates.data.actions.ActionType
import com.dawnshade.biteforce.bitecrates.placeholders.PlaceholderManager
import com.dawnshade.biteforce.bitecrates.util.FlexibleListAdaptorFactory
import com.dawnshade.biteforce.bitecrates.util.TextUtils
import com.dawnshade.biteforce.bitecrates.util.Utils
import eu.pb4.sgui.api.gui.SimpleGui
import net.minecraft.server.level.ServerPlayer

class MessageBroadcast(
    @JsonAdapter(FlexibleListAdaptorFactory::class)
    private val message: List<String> = emptyList()
) : Action(ActionType.BROADCAST) {
    override fun executeAction(player: ServerPlayer, gui: SimpleGui) {
        val parsedMessages = message.map { PlaceholderManager.parse(player, it) }

        Utils.printDebug("[ACTION - ${type.name}] Player(${player.gameProfile.name}), Parsed Messages($parsedMessages): $this")

        for (line in parsedMessages) {
            BiteCrates.INSTANCE.adventure.all().sendMessage(TextUtils.toNative(line))
        }
    }

    override fun toString(): String {
        return "MessageBroadcast(message=$message)"
    }
}
