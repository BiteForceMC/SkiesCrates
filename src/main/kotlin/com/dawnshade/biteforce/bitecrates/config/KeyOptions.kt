package com.dawnshade.biteforce.bitecrates.config

import com.google.gson.annotations.SerializedName

class KeyOptions(
    val aliases: MutableList<String> = mutableListOf(),
    @SerializedName("use_menu")
    val useMenu: Boolean = true,
) {
    override fun toString(): String {
        return "KeyOptions(aliases=$aliases, useMenu=$useMenu)"
    }
}
