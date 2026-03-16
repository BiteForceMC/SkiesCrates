package com.dawnshade.biteforce.bitecrates.config

import com.google.gson.annotations.SerializedName

class CostOptions(
    @SerializedName("provider", alternate = ["economy"])
    val provider: String? = null,
    val currency: String = "",
    val amount: Double = 0.0,
) {
    override fun toString(): String {
        return "CostOptions(provider='$provider', currency='$currency', amount=$amount)"
    }
}
