package com.dawnshade.biteforce.bitecrates.feature.opening.inventory.spinners

import com.dawnshade.biteforce.bitecrates.feature.opening.inventory.items.SpinMode
import com.dawnshade.biteforce.bitecrates.feature.opening.inventory.items.SpinningItem
import com.dawnshade.biteforce.bitecrates.gui.CrateInventory
import net.minecraft.server.level.ServerPlayer
import kotlin.random.Random

abstract class ISpinner<T>(
    protected val spinningItem: SpinningItem,
    private var isCompleted: Boolean, 
    private var isStarted: Boolean, 
    private var spinsRemaining: Int, 
    private var ticksPerSpin: Int, 
    private var ticks: Int, 
    private var ticksUntilChange: Int, 
    private var slots: MutableMap<Int, T> 
) {
    protected lateinit var pregeneratedSlots: MutableList<T> 
    private var currentIndex = 0

    
    fun tick(player: ServerPlayer, gui: CrateInventory) {
        if (isStarted) {
            ticks--
            if (ticks <= 0) {
                ticksUntilChange--
                if (ticksUntilChange <= 0) {
                    ticksUntilChange = spinningItem.changeInterval
                    ticksPerSpin += spinningItem.changeAmount
                }
                ticks = ticksPerSpin
                spinsRemaining--

                spin(player, gui)

                if (spinsRemaining <= 0) {
                    isCompleted = true
                }
            }
        } else {
            ticks--
            if (ticks <= 0) {
                isStarted = true
                ticks = ticksPerSpin

                spin(player, gui)

                if (--spinsRemaining <= 0) {
                    isCompleted = true
                }
            }
        }
    }

    
    fun pregenerate() {
        pregeneratedSlots = when (spinningItem.mode) {
            
            SpinMode.INDEPENDENT -> List(spinningItem.slots.size * spinningItem.spinCount) { generateItem() }.filterNotNull().toMutableList()

            
            SpinMode.SEQUENTIAL, SpinMode.SYNCED -> List(spinningItem.spinCount) { generateItem() }.filterNotNull().toMutableList()

            
            SpinMode.RANDOM -> {
                val list: MutableList<T> = mutableListOf()

                val tempList: MutableList<T> =  List(spinningItem.slots.size) { generateItem() }
                    .filterNotNull()
                    .toMutableList()
                
                for (i in 0..<spinningItem.spinCount) {
                    generateItem()?.let {
                        val slot = Random.nextInt(spinningItem.slots.size)
                        tempList[slot] = it
                        list.addAll(tempList)
                    }
                }

                list
            }
        }
    }

    
    private fun spin(player: ServerPlayer, gui: CrateInventory) {
        when (spinningItem.mode) {
            SpinMode.INDEPENDENT, SpinMode.RANDOM -> {
                
                spinningItem.slots.forEach { slot ->
                    val value = pregeneratedSlots[currentIndex++]
                    slots[slot] = value
                    updateSlot(gui, slot, value)
                }
            }
            SpinMode.SEQUENTIAL -> {
                
                for (i in spinningItem.slots.size - 1 downTo 1) {
                    val slot = spinningItem.slots[i]
                    val tempReward = slots[spinningItem.slots[i - 1]] ?: continue
                    slots[slot] = tempReward
                    updateSlot(gui, slot, tempReward)
                }
                val value = pregeneratedSlots[currentIndex++]
                val slot = spinningItem.slots.first()
                slots[slot] = value
                updateSlot(gui, slot, value)
            }
            SpinMode.SYNCED -> {
                
                val value = pregeneratedSlots[currentIndex++]
                spinningItem.slots.forEach { slot ->
                    slots[slot] = value
                    updateSlot(gui, slot, value)
                }
            }
        }
        spinningItem.sound?.playSound(player)
    }

    fun isCompleted(): Boolean {
        return isCompleted
    }

    
    
    abstract fun updateSlot(gui: CrateInventory, slot: Int, value: T)

    
    abstract fun generateItem(): T?
}
