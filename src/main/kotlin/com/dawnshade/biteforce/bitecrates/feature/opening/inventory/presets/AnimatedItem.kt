package com.dawnshade.biteforce.bitecrates.feature.opening.inventory.presets

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.dawnshade.biteforce.bitecrates.core.BiteCrates
import com.dawnshade.biteforce.bitecrates.util.FlexibleListAdaptorFactory
import com.dawnshade.biteforce.bitecrates.util.TextUtils
import com.dawnshade.biteforce.bitecrates.util.Utils
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ItemLore

class AnimatedItem(
    val weight: Int = 1,
    val item: String = "",
    val amount: Int = 1,
    val name: String? = null,
    @JsonAdapter(FlexibleListAdaptorFactory::class)
    val lore: List<String> = emptyList(),
    @SerializedName("nbt", alternate = ["components"])
    val nbt: CompoundTag? = null
) {
    fun createItemStack(player: ServerPlayer): ItemStack {
        if (item.isEmpty()) return ItemStack(Items.BARRIER, amount)

        val parsedItem = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(item))

        if (parsedItem.isEmpty) {
            Utils.printError("Error while getting Item, defaulting to Barrier: $item")
            return ItemStack(Items.BARRIER, amount)
        }

        val stack = ItemStack(parsedItem.get(), amount)

        if (nbt != null) {
            
            val nbtCopy = nbt.copy()
            for (key in nbt.allKeys) {
                val element = nbt.get(key)
                if (element != null) {
                    if (element is StringTag) {
                        nbtCopy.putString(key, element.asString)
                    } else if (element is ListTag) {
                        val parsed = ListTag()
                        for (entry in element) {
                            if (entry is StringTag) {
                                parsed.add(StringTag.valueOf(entry.asString))
                            } else {
                                parsed.add(entry)
                            }
                        }
                        nbtCopy.put(key, parsed)
                    }
                }
            }

            DataComponentPatch.CODEC.decode(BiteCrates.INSTANCE.nbtOpts, nbtCopy).result().ifPresent { result ->
                stack.applyComponents(result.first)
            }
        }

        val dataComponents = DataComponentPatch.builder()

        if (name != null) {
            dataComponents.set(
                DataComponents.ITEM_NAME, Component.empty().setStyle(Style.EMPTY.withItalic(false))
                .append(TextUtils.toNative(name)))
        }

        if (lore.isNotEmpty()) {
            val parsedLore: MutableList<String> = mutableListOf()
            for (line in lore.stream().map { it }.toList()) {
                if (line.contains("\n")) {
                    line.split("\n").forEach { parsedLore.add(it) }
                } else {
                    parsedLore.add(line)
                }
            }
            dataComponents.set(DataComponents.LORE, ItemLore(parsedLore.stream().map {
                Component.empty().setStyle(Style.EMPTY.withItalic(false)).append(TextUtils.toNative(it)) as Component
            }.toList()))
        }

        stack.applyComponents(dataComponents.build())

        return stack
    }

    override fun toString(): String {
        return "AnimatedItem(weight=$weight, item=$item, amount=$amount, name=$name, lore=$lore, nbt=$nbt)"
    }
}
