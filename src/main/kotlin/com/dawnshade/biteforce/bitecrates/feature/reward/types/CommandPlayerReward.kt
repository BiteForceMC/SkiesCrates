package com.dawnshade.biteforce.bitecrates.feature.reward.types

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.dawnshade.biteforce.bitecrates.core.BiteCrates
import com.dawnshade.biteforce.bitecrates.config.item.GenericItem
import com.dawnshade.biteforce.bitecrates.data.Crate
import com.dawnshade.biteforce.bitecrates.feature.reward.Reward
import com.dawnshade.biteforce.bitecrates.feature.reward.RewardLimits
import com.dawnshade.biteforce.bitecrates.feature.reward.RewardType
import com.dawnshade.biteforce.bitecrates.feature.reward.types.CommandConsoleReward.Companion.DEFAULT_DISPLAY
import com.dawnshade.biteforce.bitecrates.placeholders.PlaceholderManager
import com.dawnshade.biteforce.bitecrates.util.FlexibleListAdaptorFactory
import net.minecraft.server.level.ServerPlayer

class CommandPlayerReward(
    name: String = "",
    display: GenericItem? = null,
    weight: Int = 1,
    limits: RewardLimits? = null,
    broadcast: Boolean = false,
    @JsonAdapter(FlexibleListAdaptorFactory::class) @SerializedName("commands",  alternate = ["command"])
    private val commands: List<String> = emptyList(),
    @SerializedName("permission_level")
    private val permissionLevel: Int? = null
) : Reward(RewardType.COMMAND_PLAYER, name, display, weight, limits, broadcast) {
    override fun giveReward(player: ServerPlayer, crate: Crate): Boolean {
        if (!super.giveReward(player, crate)) {
            return false
        }
        val parsedCommands = commands.map { PlaceholderManager.parse(player, it) }

        var source = player.createCommandSourceStack()
        if (permissionLevel != null) {
            source = source.withPermission(permissionLevel)
        }

        return try {
            for (command in parsedCommands) {
                BiteCrates.INSTANCE.server.commands.performPrefixedCommand(
                    source,
                    command
                )
            }
            true
        } catch (exception: Exception) {
            com.dawnshade.biteforce.bitecrates.util.Utils.printError("Failed to execute player reward '$id' for ${player.name.string}: ${exception.message}")
            false
        }
    }

    override fun getGenericDisplay(): GenericItem {
        return display ?: DEFAULT_DISPLAY
    }

    override fun toString(): String {
        return "CommandPlayer(type=$type, name='$name', display=$display, weight=$weight, limits=$limits, broadcast=$broadcast, commands=$commands, permissionLevel=$permissionLevel)"
    }
}
