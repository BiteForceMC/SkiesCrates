package com.dawnshade.biteforce.bitecrates.feature.reward

import com.dawnshade.biteforce.bitecrates.feature.reward.types.CommandConsoleReward
import com.dawnshade.biteforce.bitecrates.feature.reward.types.CommandPlayerReward
import com.dawnshade.biteforce.bitecrates.feature.reward.types.ItemReward
import com.dawnshade.biteforce.bitecrates.feature.reward.types.PokemonReward

enum class RewardType(val identifier: String, val clazz: Class<*>, val dependencies: List<String> = emptyList()) {
    COMMAND_CONSOLE("command_console", CommandConsoleReward::class.java),
    COMMAND_PLAYER("command_player", CommandPlayerReward::class.java),
    ITEM("item", ItemReward::class.java),
    POKEMON("pokemon", PokemonReward::class.java, listOf("cobblemon"));

    companion object {
        fun valueOfAnyCase(name: String): RewardType? {
            for (type in entries) {
                if (name.equals(type.identifier, true)) return type
            }
            return null
        }
    }
}
