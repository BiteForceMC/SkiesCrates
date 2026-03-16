package com.dawnshade.biteforce.bitecrates.data.previews

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.dawnshade.biteforce.bitecrates.config.item.ActionMenuItem
import com.dawnshade.biteforce.bitecrates.data.Crate
import com.dawnshade.biteforce.bitecrates.feature.reward.Reward
import com.dawnshade.biteforce.bitecrates.state.userdata.UserData
import com.dawnshade.biteforce.bitecrates.gui.InventoryType
import com.dawnshade.biteforce.bitecrates.util.FlexibleListAdaptorFactory
import com.dawnshade.biteforce.bitecrates.util.TextUtils
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemLore

class Preview(
    val title: String,
    @SerializedName("type", alternate = ["menu_type"])
    val type: InventoryType,
    val rewards: RewardButton,
    val items: MutableMap<String, ActionMenuItem>,
) {
    class RewardButton(
        @JsonAdapter(FlexibleListAdaptorFactory::class)
        val slots: List<Int> = emptyList(),
        val name: String? = null,
        @JsonAdapter(FlexibleListAdaptorFactory::class)
        val lore: List<String> = emptyList()
    ) {
        fun createItemStack(player: ServerPlayer, reward: Reward, crate: Crate, userData: UserData): ItemStack {
            val placeholders = reward.getPlaceholders(userData, crate)

            
            val item: ItemStack = reward.preview?.let { guiItem ->
                return@let guiItem.createItemStack(player, placeholders)
            } ?: reward.getDisplayItem(player, placeholders)

            val dataComponents = DataComponentPatch.builder()

            
            (reward.preview?.name ?: name)?.let { name ->
                dataComponents.set(
                    DataComponents.ITEM_NAME, Component.empty().setStyle(Style.EMPTY.withItalic(false))
                        .append(TextUtils.toNative(
                            name.let {  placeholders.entries.fold(it) { acc, (key, value) -> acc.replace(key, value) } }
                        )))
            }

            (reward.preview?.lore ?: lore).let { lore ->
                if (lore.isNotEmpty()) {
                    val parsedLore: MutableList<String> = mutableListOf()
                    for (line in lore.stream().map { it }.toList()) {
                        val parsedLine = line.let { placeholders.entries.fold(it) { acc, (key, value) -> acc.replace(key, value) } }
                        if (parsedLine.contains("\n")) {
                            parsedLine.split("\n").forEach { parsedLore.add(it) }
                        } else {
                            parsedLore.add(parsedLine)
                        }
                    }
                    dataComponents.set(DataComponents.LORE, ItemLore(parsedLore.stream().map {
                        Component.empty().setStyle(Style.EMPTY.withItalic(false)).append(TextUtils.toNative(
                            it
                        )) as Component
                    }.toList()))
                }
            }

            item.applyComponents(dataComponents.build())

            return item
        }
    }
}
