package com.dawnshade.biteforce.bitecrates.gui

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import net.minecraft.world.inventory.MenuType
import java.lang.reflect.Type

enum class InventoryType(val type: MenuType<*>) {
    GENERIC_9x1(MenuType.GENERIC_9x1),
    GENERIC_9x2(MenuType.GENERIC_9x2),
    GENERIC_9x3(MenuType.GENERIC_9x3),
    GENERIC_9x4(MenuType.GENERIC_9x4),
    GENERIC_9x5(MenuType.GENERIC_9x5),
    GENERIC_9x6(MenuType.GENERIC_9x6),
    GENERIC_3x3(MenuType.GENERIC_3x3),
    CRAFTER_3x3(MenuType.CRAFTER_3x3),
    ANVIL(MenuType.ANVIL),
    BEACON(MenuType.BEACON),
    BLAST_FURNACE(MenuType.BLAST_FURNACE),
    BREWING_STAND(MenuType.BREWING_STAND),
    CRAFTING(MenuType.CRAFTING),
    ENCHANTMENT(MenuType.ENCHANTMENT),
    FURNACE(MenuType.FURNACE),
    GRINDSTONE(MenuType.GRINDSTONE),
    HOPPER(MenuType.HOPPER),
    LECTERN(MenuType.LECTERN),
    LOOM(MenuType.LOOM),
    MERCHANT(MenuType.MERCHANT),
    SHULKER_BOX(MenuType.SHULKER_BOX),
    SMITHING(MenuType.SMITHING),
    SMOKER(MenuType.SMOKER),
    CARTOGRAPHY_TABLE(MenuType.CARTOGRAPHY_TABLE),
    STONECUTTER(MenuType.STONECUTTER);

    class Deserializer : JsonDeserializer<InventoryType> {
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): InventoryType {
            val value = json?.asString
            return entries.find { it.name.equals(value, true) } ?: GENERIC_9x6
        }
    }
}
