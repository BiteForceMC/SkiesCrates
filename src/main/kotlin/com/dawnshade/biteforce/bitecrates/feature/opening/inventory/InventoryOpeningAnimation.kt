package com.dawnshade.biteforce.bitecrates.feature.opening.inventory

import com.google.gson.annotations.SerializedName
import com.dawnshade.biteforce.bitecrates.config.item.MenuItem
import com.dawnshade.biteforce.bitecrates.feature.opening.OpeningAnimation
import com.dawnshade.biteforce.bitecrates.feature.opening.inventory.items.SpinningItem
import com.dawnshade.biteforce.bitecrates.feature.opening.inventory.presets.AnimatedItem
import com.dawnshade.biteforce.bitecrates.feature.opening.inventory.presets.RewardItem
import com.dawnshade.biteforce.bitecrates.gui.InventoryType

class InventoryOpeningAnimation(
    val title: String,
    @SerializedName("type", alternate = ["menu_type"])
    val type: InventoryType,
    @SerializedName("close_delay")
    val closeDelay: Int,
    @SerializedName("win_slots")
    val winSlots: List<Int>,
    val skippable: Boolean = false,
    val items: Items,
    val presets: Presets
): OpeningAnimation {
    
    class Items(
        
        val rewards: MutableMap<String, SpinningItem>,
        
        val animated: MutableMap<String, SpinningItem>,
        
        val static: MutableMap<String, MenuItem>
    )

    class Presets(
        val rewards: MutableMap<String, RewardItem>,
        val animations: MutableMap<String, List<AnimatedItem>>,
    )
}