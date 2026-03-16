package com.dawnshade.biteforce.bitecrates.data

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.dawnshade.biteforce.bitecrates.config.ConfigManager
import com.dawnshade.biteforce.bitecrates.config.CostOptions
import com.dawnshade.biteforce.bitecrates.config.FailureOptions
import com.dawnshade.biteforce.bitecrates.config.block.BlockOptions
import com.dawnshade.biteforce.bitecrates.config.item.GenericItem
import com.dawnshade.biteforce.bitecrates.feature.reward.Reward
import com.dawnshade.biteforce.bitecrates.state.userdata.UserData
import com.dawnshade.biteforce.bitecrates.util.RandomCollection

class Crate(
    val enabled: Boolean = true,
    val name: String = "",
    val display: GenericItem = GenericItem(),
    val unique: Boolean = false,
    val preview: String = "",
    val animation: String = "",
    val permission: String = "",
    @SerializedName("inventory_space")
    val inventorySpace: Int = -1,
    val cost: CostOptions? = null,
    val cooldown: Long = -1,
    val failure: FailureOptions? = null,
    val keys: Map<String, Int> = emptyMap(),
    val block: BlockOptions = BlockOptions(),
    @JsonAdapter(Reward.RewardMapAdapter::class)
    val rewards: MutableMap<String, Reward> = mutableMapOf(),
) {
    lateinit var id: String

    fun parsePlaceholders(string: String): String {
        return string.replace("%crate_name%", name)
            .replace("%crate_id%", id)
            .replace("%crate_keys%", keys.entries.joinToString(", ") { (keyId, amount) ->
                "${ConfigManager.KEYS[keyId]?.name ?: keyId} x$amount"
            })
            .replace("%crate_inventory_space%", inventorySpace.toString())
    }

    fun generateRewardBag(data: UserData): RandomCollection<Reward> {
        val bag = RandomCollection<Reward>()
        val validRewards = rewards.values.filter { reward ->
            reward.canReceive(data, this)
        }
        val unseenRewards = validRewards.filter { reward ->
            data.getRewardLimits(this, reward) <= 0
        }
        val selectedRewards = if (unseenRewards.isNotEmpty()) unseenRewards else validRewards
        for (reward in selectedRewards) {
            bag.add(reward, reward.weight.toDouble())
        }

        return bag
    }

    override fun toString(): String {
        return "Crate(id='$id', enabled=$enabled, name='$name', display=$display, unique=$unique, preview='$preview', " +
                "animation='$animation', permission='$permission', cost=$cost, cooldown=$cooldown, keys=$keys, " +
                "block=$block, rewards=$rewards)"
    }
}
