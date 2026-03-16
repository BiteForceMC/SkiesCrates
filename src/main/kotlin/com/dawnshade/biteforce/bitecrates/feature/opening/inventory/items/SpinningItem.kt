package com.dawnshade.biteforce.bitecrates.feature.opening.inventory.items

import com.google.gson.annotations.SerializedName
import com.dawnshade.biteforce.bitecrates.config.SoundOption

class SpinningItem(
    val preset: String, 
    val mode: SpinMode, 
    val slots: List<Int>, 
    @SerializedName("spin_count")
    val spinCount: Int, 
    @SerializedName("spin_interval")
    val spinInterval: Int, 
    @SerializedName("start_delay")
    val startDelay: Int, 
    @SerializedName("change_interval")
    val changeInterval: Int, 
    @SerializedName("change_amount")
    val changeAmount: Int, 
    val sound: SoundOption?, 
)
