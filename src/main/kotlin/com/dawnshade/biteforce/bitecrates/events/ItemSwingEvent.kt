package com.dawnshade.biteforce.bitecrates.events

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.ItemStack

fun interface ItemSwingEvent {
    companion object {
        @JvmField
        val EVENT: Event<ItemSwingEvent> =
            EventFactory.createArrayBacked(ItemSwingEvent::class.java) { listeners ->
                ItemSwingEvent { player, itemStack, hand ->
                    for (listener in listeners) {
                        val result = listener.interact(player, itemStack, hand)

                        if (result != InteractionResult.PASS) {
                            return@ItemSwingEvent result
                        }
                    }

                    return@ItemSwingEvent InteractionResult.PASS
                }
            }
    }

    fun interact(player: ServerPlayer, itemStack: ItemStack, hand: InteractionHand): InteractionResult
}