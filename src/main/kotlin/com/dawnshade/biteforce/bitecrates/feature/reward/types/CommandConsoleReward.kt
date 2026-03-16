package com.dawnshade.biteforce.bitecrates.feature.reward.types

import com.google.gson.annotations.JsonAdapter
import com.dawnshade.biteforce.bitecrates.core.BiteCrates
import com.dawnshade.biteforce.bitecrates.config.item.GenericItem
import com.dawnshade.biteforce.bitecrates.data.Crate
import com.dawnshade.biteforce.bitecrates.feature.reward.Reward
import com.dawnshade.biteforce.bitecrates.feature.reward.RewardLimits
import com.dawnshade.biteforce.bitecrates.feature.reward.RewardType
import com.dawnshade.biteforce.bitecrates.placeholders.PlaceholderManager
import com.dawnshade.biteforce.bitecrates.util.FlexibleListAdaptorFactory
import com.dawnshade.biteforce.bitecrates.util.Utils
import net.minecraft.server.level.ServerPlayer

class CommandConsoleReward(
    name: String = "",
    weight: Int = 1,
    display: GenericItem? = null,
    limits: RewardLimits? = null,
    broadcast: Boolean = false,
    @JsonAdapter(FlexibleListAdaptorFactory::class)
    private val commands: List<String> = emptyList()
) : Reward(RewardType.COMMAND_CONSOLE, name, display, weight, limits, broadcast) {
    companion object {
        val DEFAULT_DISPLAY = GenericItem("minecraft:paper", name = "Command")
    }

    override fun giveReward(player: ServerPlayer, crate: Crate): Boolean {
        if (!super.giveReward(player, crate)) {
            return false
        }
        if (BiteCrates.INSTANCE.server.commands == null) {
            Utils.printError("There was an error while giving a reward for player ${player.name}: Server was somehow null on command execution?")
            return false
        }
        return try {
            for (command in commands) {
                BiteCrates.INSTANCE.server.commands.performPrefixedCommand(
                    BiteCrates.INSTANCE.server.createCommandSourceStack(),
                    PlaceholderManager.parse(player, command)
                )
            }
            true
        } catch (exception: Exception) {
            Utils.printError("Failed to execute console reward '$id' for ${player.name.string}: ${exception.message}")
            false
        }
    }

    override fun getGenericDisplay(): GenericItem {
        return display ?: DEFAULT_DISPLAY
    }

    override fun toString(): String {
        return "CommandConsole(type=$type, name='$name', display=$display, weight=$weight, limits=$limits, broadcast=$broadcast, commands=$commands)"
    }
}
