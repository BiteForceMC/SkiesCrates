package com.dawnshade.biteforce.bitecrates.data.actions.types

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.dawnshade.biteforce.bitecrates.core.BiteCrates
import com.dawnshade.biteforce.bitecrates.data.actions.Action
import com.dawnshade.biteforce.bitecrates.data.actions.ActionType
import com.dawnshade.biteforce.bitecrates.util.FlexibleListAdaptorFactory
import com.dawnshade.biteforce.bitecrates.util.Utils
import eu.pb4.sgui.api.gui.SimpleGui
import net.minecraft.server.level.ServerPlayer

class CommandPlayer(
    @JsonAdapter(FlexibleListAdaptorFactory::class)
    private val commands: List<String> = emptyList(),
    @SerializedName("permission_level")
    private val permissionLevel: Int? = null
) : Action(ActionType.COMMAND_PLAYER) {
    override fun executeAction(player: ServerPlayer, gui: SimpleGui) {
        val parsedCommands = commands.map { it  }

        var source = player.createCommandSourceStack()
        if (permissionLevel != null) {
            source = source.withPermission(permissionLevel)
        }

        Utils.printDebug("[ACTION - ${type.name}] Player(${player.gameProfile.name}), Parsed Commands($parsedCommands): $this")

        for (command in parsedCommands) {
            BiteCrates.INSTANCE.server.commands.performPrefixedCommand(
                source,
                command
            )
        }
    }

    override fun toString(): String {
        return "CommandPlayer(commands=$commands, permissionLevel=$permissionLevel)"
    }
}
