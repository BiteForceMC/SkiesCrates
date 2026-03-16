package com.dawnshade.biteforce.bitecrates.placeholders

import net.fabricmc.loader.api.FabricLoader

enum class PlaceholderMods(val modId: String) {
    IMPACTOR("impactor"),
    PLACEHOLDERAPI("placeholder-api"),
    MINIPLACEHOLDERS("miniplaceholders");

    fun isModPresent() : Boolean {
        return FabricLoader.getInstance().isModLoaded(modId)
    }
}
