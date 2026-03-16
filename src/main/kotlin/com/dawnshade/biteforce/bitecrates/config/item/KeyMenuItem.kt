package com.dawnshade.biteforce.bitecrates.config.item

import com.dawnshade.biteforce.bitecrates.feature.key.Key
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack

class KeyMenuItem(
    item: String = "",
    slots: List<Int> = emptyList(),
    amount: Int = 1,
    name: String? = null,
    lore: List<String> = emptyList(),
    components: CompoundTag? = null,
    customModelData: Int? = null,
): MenuItem(item, slots, amount, name, lore, components, customModelData) {
    fun createItemStack(player: ServerPlayer, key: Key, count: Int): ItemStack {
        return createItemStack(player, mapOf(
            "%key_amount%" to count.toString(),
            "%key_id%" to key.id
        ))
    }

    override fun toString(): String {
        return "KeyMenuItem(item=$item, slots=$slots, amount=$amount, name=$name, lore=$lore, components=$components, customModelData=$customModelData)"
    }
}