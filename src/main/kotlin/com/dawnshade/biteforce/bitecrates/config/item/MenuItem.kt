package com.dawnshade.biteforce.bitecrates.config.item

import com.google.gson.annotations.JsonAdapter
import com.dawnshade.biteforce.bitecrates.util.FlexibleListAdaptorFactory
import net.minecraft.nbt.CompoundTag

open class MenuItem(
    item: String = "minecraft:air",
    @JsonAdapter(FlexibleListAdaptorFactory::class)
    val slots: List<Int> = emptyList(),
    amount: Int = 1,
    name: String? = null,
    lore: List<String> = emptyList(),
    components: CompoundTag? = null,
    customModelData: Int? = null,
): GenericItem(item, amount, name, lore, components, customModelData) {
    override fun toString(): String {
        return "MenuItem(item=$item, slots=$slots, amount=$amount, name=$name, lore=$lore, components=$components, customModelData=$customModelData)"
    }
}