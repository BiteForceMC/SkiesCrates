package com.pokeskies.skiescrates.data.rewards.types

import com.pokeskies.skiescrates.config.item.GenericItem
import com.pokeskies.skiescrates.data.Crate
import com.pokeskies.skiescrates.data.rewards.Reward
import com.pokeskies.skiescrates.data.rewards.RewardLimits
import com.pokeskies.skiescrates.data.rewards.RewardType
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
    override fun giveReward(player: ServerPlayer, crate: Crate) {
        // Super to call the message
        super.giveReward(player, crate)

        val rewardItem = item.createItemStack(player)
        rewardItem.remove(DataComponents.CUSTOM_NAME)
        rewardItem.remove(DataComponents.ITEM_NAME)
        player.inventory.placeItemBackInInventory(rewardItem)
    }

    override fun getGenericDisplay(): GenericItem {
        return display ?: item
    }

    override fun toString(): String {
        return "ItemReward(name='$name', display=$display, weight=$weight, limits=$limits, broadcast=$broadcast, item=$item)"
    }
}
