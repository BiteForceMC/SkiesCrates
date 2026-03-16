package com.dawnshade.biteforce.bitecrates.feature.opening.inventory.spinners

import com.dawnshade.biteforce.bitecrates.feature.opening.inventory.items.SpinningItem
import com.dawnshade.biteforce.bitecrates.gui.CrateInventory
import com.dawnshade.biteforce.bitecrates.util.RandomCollection
import net.minecraft.world.item.ItemStack

class AnimatedSpinnerInstance(
    spinningItem: SpinningItem,
    isCompleted: Boolean, 
    isStarted: Boolean, 
    spinsRemaining: Int, 
    ticksPerSpin: Int, 
    ticks: Int, 
    ticksUntilChange: Int, 
    items: MutableMap<Int, ItemStack>, 
    private val randomBag: RandomCollection<ItemStack>,
): ISpinner<ItemStack>(spinningItem, isCompleted, isStarted, spinsRemaining, ticksPerSpin, ticks, ticksUntilChange, items) {
    constructor(
        spinningItem: SpinningItem,
        randomBag: RandomCollection<ItemStack>,
    ) : this(
        spinningItem,
        false,
        false,
        spinningItem.spinCount,
        spinningItem.spinInterval,
        ticks = if (spinningItem.startDelay > 0) spinningItem.startDelay else spinningItem.spinInterval,
        spinningItem.changeInterval,
        mutableMapOf(),
        randomBag,
    )

    override fun generateItem(): ItemStack? {
        return randomBag.next()
    }

    override fun updateSlot(gui: CrateInventory, slot: Int, value: ItemStack) {
        if (slot > gui.size || slot < 0) return
        gui.setSlot(slot, value)
    }
}
