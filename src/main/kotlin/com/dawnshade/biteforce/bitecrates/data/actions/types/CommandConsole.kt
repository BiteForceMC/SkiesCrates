package com.dawnshade.biteforce.bitecrates.data.actions.types

import com.google.gson.annotations.JsonAdapter
import com.dawnshade.biteforce.bitecrates.core.BiteCrates
import com.dawnshade.biteforce.bitecrates.data.actions.Action
import com.dawnshade.biteforce.bitecrates.data.actions.ActionType
import com.dawnshade.biteforce.bitecrates.util.FlexibleListAdaptorFactory
import com.dawnshade.biteforce.bitecrates.util.Utils
import eu.pb4.sgui.api.gui.SimpleGui
import net.minecraft.server.level.ServerPlayer

class CommandConsole(
    @JsonAdapter(FlexibleListAdaptorFactory::class)
    private val commands: List<String> = emptyList()
) : Action(ActionType.COMMAND_CONSOLE) {
    override fun executeAction(player: ServerPlayer, gui: SimpleGui) {
        val parsedCommands = commands.map { it  }

        Utils.printDebug("[ACTION - ${type.name}] Player(${player.gameProfile.name}), Parsed Commands($parsedCommands): $this")

        for (command in parsedCommands) {
            BiteCrates.INSTANCE.server.commands.performPrefixedCommand(
                BiteCrates.INSTANCE.server.createCommandSourceStack(),
                command
            )
        }
    }

    override fun toString(): String {
        return "CommandConsole(commands=$commands)"
    }
}
