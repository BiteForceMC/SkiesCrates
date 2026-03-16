package com.dawnshade.biteforce.bitecrates.gui

import com.dawnshade.biteforce.bitecrates.core.BiteCrates
import com.dawnshade.biteforce.bitecrates.data.Crate
import com.dawnshade.biteforce.bitecrates.data.previews.Preview
import com.dawnshade.biteforce.bitecrates.feature.reward.Reward
import com.dawnshade.biteforce.bitecrates.util.TextUtils
import eu.pb4.sgui.api.elements.GuiElementBuilder
import eu.pb4.sgui.api.gui.SimpleGui
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack

class PreviewInventory(player: ServerPlayer, val crate: Crate, val preview: Preview): SimpleGui(
    preview.type.type, player, false
) {
    private val rewards: MutableMap<String, Pair<Reward, ItemStack>> = mutableMapOf()

    private val pageSlots = preview.rewards.slots.size.takeIf { it > 0 } ?: 1
    private var page = 0
    private var maxPages = 1

    init {
        this.title = TextUtils.parseAllNative(player, crate.parsePlaceholders(preview.title))

        val userData = BiteCrates.INSTANCE.storage.getUser(player)

        preview.items.forEach { (id, item) ->
            item.createItemStack(player).let {
                item.slots.forEach { slot ->
                    this.setSlot(slot, GuiElementBuilder(it)
                        .setCallback { _ ->
                            item.actions.forEach { (id, action) ->
                                action.executeAction(player, this)
                            }
                        }
                    )
                }
            }
        }

        crate.rewards.forEach { (id, reward) ->
            rewards[id] = reward to preview.rewards.createItemStack(player, reward, crate, userData)
        }

        maxPages = (rewards.size + pageSlots - 1) / pageSlots

        renderRewards()
    }

    fun nextPage() {
        if (page < (maxPages - 1)) {
            page++
            renderRewards()
        }
    }

    fun previousPage() {
        if (page > 0) {
            page--
            renderRewards()
        }
    }

    private fun renderRewards() {
        preview.rewards.slots.forEach { slot ->
            this.clearSlot(slot)
        }
        var index = 0
        for ((_, pair) in rewards.toList().subList(pageSlots * page, minOf(pageSlots * (page + 1), rewards.size))) {
            if (index < pageSlots) {
                GuiElementBuilder.from(pair.second).let {
                    this.setSlot(preview.rewards.slots[index++], it)
                }
            }
        }
    }
}
