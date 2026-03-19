package com.dawnshade.biteforce.bitecrates.feature.reward.types

import com.dawnshade.biteforce.bitecrates.config.item.GenericItem
import com.dawnshade.biteforce.bitecrates.data.Crate
import com.dawnshade.biteforce.bitecrates.feature.reward.Reward
import com.dawnshade.biteforce.bitecrates.feature.reward.RewardLimits
import com.dawnshade.biteforce.bitecrates.feature.reward.RewardType
import net.minecraft.core.component.DataComponents
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

        val rewardItem = item.createItemStack(player)
        rewardItem.remove(DataComponents.CUSTOM_NAME)
        rewardItem.remove(DataComponents.ITEM_NAME)
        player.inventory.placeItemBackInInventory(rewardItem)
        return true
    }

    override fun getGenericDisplay(): GenericItem {
        return display ?: item
    }

    override fun toString(): String {
        return "ItemReward(name='$name', display=$display, weight=$weight, limits=$limits, broadcast=$broadcast, item=$item)"
    }
}
