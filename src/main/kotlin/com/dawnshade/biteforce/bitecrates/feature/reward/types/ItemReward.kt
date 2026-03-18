package com.dawnshade.biteforce.bitecrates.feature.reward.types

import com.dawnshade.biteforce.bitecrates.config.ConfigManager
import com.dawnshade.biteforce.bitecrates.config.item.GenericItem
import com.dawnshade.biteforce.bitecrates.data.Crate
import com.dawnshade.biteforce.bitecrates.feature.reward.Reward
import com.dawnshade.biteforce.bitecrates.feature.reward.RewardLimits
import com.dawnshade.biteforce.bitecrates.feature.reward.RewardType
import net.minecraft.server.level.ServerPlayer

class ItemReward(
    name: String = "",
    display: GenericItem? = null,
    weight: Int = 1,
    limits: RewardLimits? = null,
    broadcast: Boolean = false,
    private val item: GenericItem = GenericItem()
) : Reward(RewardType.ITEM, name, display, weight, limits, broadcast) {
    override fun giveReward(player: ServerPlayer, crate: Crate): Boolean {
        if (!super.giveReward(player, crate)) {
            return false
        }

        val stack = if (ConfigManager.CONFIG.itemRewardsUseDisplayName && item.name == null && name.isNotBlank()) {
            item.createItemStack(player, fallbackName = name)
        } else {
            item.createItemStack(player)
        }
        player.inventory.placeItemBackInInventory(stack)
        return true
    }

    override fun getGenericDisplay(): GenericItem {
        return display ?: item
    }

    override fun toString(): String {
        return "ItemReward(name='$name', display=$display, weight=$weight, limits=$limits, broadcast=$broadcast, item=$item)"
    }
}
