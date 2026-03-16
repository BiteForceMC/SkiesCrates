package com.dawnshade.biteforce.bitecrates.feature.opening.inventory.spinners

import com.dawnshade.biteforce.bitecrates.data.Crate
import com.dawnshade.biteforce.bitecrates.feature.opening.inventory.items.SpinMode
import com.dawnshade.biteforce.bitecrates.feature.opening.inventory.items.SpinningItem
import com.dawnshade.biteforce.bitecrates.feature.reward.Reward
import com.dawnshade.biteforce.bitecrates.state.userdata.UserData
import com.dawnshade.biteforce.bitecrates.gui.CrateInventory
import com.dawnshade.biteforce.bitecrates.util.RandomCollection
import net.minecraft.server.level.ServerPlayer

class RewardSpinnerInstance(
    spinningItem: SpinningItem,
    isCompleted: Boolean,
    isStarted: Boolean,
    spinsRemaining: Int,
    ticksPerSpin: Int,
    ticks: Int,
    ticksUntilChange: Int,
    rewards: MutableMap<Int, Reward>,
    private val randomBag: RandomCollection<Reward>,
    private val winSlots: List<Int>,
): ISpinner<Reward>(spinningItem, isCompleted, isStarted, spinsRemaining, ticksPerSpin, ticks, ticksUntilChange, rewards) {
    constructor(
        spinningItem: SpinningItem,
        rewardBag: RandomCollection<Reward>,
        winSlots: List<Int>,
    ) : this(
        spinningItem,
        false,
        false,
        spinningItem.spinCount,
        spinningItem.spinInterval,
        ticks = if (spinningItem.startDelay > 0) spinningItem.startDelay else spinningItem.spinInterval,
        spinningItem.changeInterval,
        mutableMapOf(),
        rewardBag,
        winSlots
    )

    override fun generateItem(): Reward? {
        if (randomBag.size() <= 0) return null
        return randomBag.next()
    }

    override fun updateSlot(gui: CrateInventory, slot: Int, value: Reward) {
        gui.updateRewardSlot(slot, value)
    }

    fun getFinalRewards(): List<Reward> {
        if (pregeneratedSlots.isEmpty()) return emptyList()
        return when (spinningItem.mode) {
            SpinMode.RANDOM, SpinMode.SEQUENTIAL, SpinMode.INDEPENDENT -> {
                val winIndexes = winSlots.map { spinningItem.slots.indexOf(it) }.filter { it >= 0 }

                val winRewards = winIndexes.map { index -> pregeneratedSlots[(pregeneratedSlots.size - 1) - index] }

                winRewards
            }
            SpinMode.SYNCED -> {
                val reward = pregeneratedSlots.lastOrNull() ?: return emptyList()
                List(winSlots.size) { reward }
            }
        }
    }

    fun giveRewards(player: ServerPlayer, crate: Crate): List<Reward>? {
        val finalRewards = getFinalRewards()
        for (reward in finalRewards) {
            if (!reward.giveReward(player, crate)) {
                return null
            }
        }
        return finalRewards
    }

    fun validateRewards(crate: Crate, userData: UserData, reservedRewardIds: MutableSet<String>): RandomCollection<Reward>? {
        val availableRewards = randomBag.copy()
        val rewards = getFinalRewards()
        for ((index, reward) in rewards.withIndex()) {
            val replacement = selectReward(crate, userData, availableRewards, reservedRewardIds, reward) ?: return null
            setFinalReward(index, replacement)
            reservedRewardIds += replacement.id
            if (availableRewards.size() > 1) {
                availableRewards.remove(replacement)
            }
        }
        return availableRewards
    }

    private fun selectReward(
        crate: Crate,
        userData: UserData,
        availableRewards: RandomCollection<Reward>,
        reservedRewardIds: Set<String>,
        currentReward: Reward,
    ): Reward? {
        var selectedReward: Reward? = currentReward.takeIf { reward ->
            reward.canReceive(userData, crate) && reward.id !in reservedRewardIds
        }
        if (selectedReward != null) {
            return selectedReward
        }
        val selectionBag = availableRewards.copy()
        selectionBag.entries().keys.filter { reward ->
            reward.id in reservedRewardIds || !reward.canReceive(userData, crate)
        }.forEach { reward ->
            selectionBag.remove(reward)
        }
        selectedReward = selectionBag.next()
        return selectedReward ?: currentReward.takeIf { reward ->
            reward.canReceive(userData, crate)
        }
    }

    private fun setFinalReward(rewardIndex: Int, reward: Reward) {
        val index = when (spinningItem.mode) {
            SpinMode.RANDOM, SpinMode.SEQUENTIAL, SpinMode.INDEPENDENT -> {
                (pregeneratedSlots.size - 1) - spinningItem.slots.indexOf(winSlots[rewardIndex])
            }
            SpinMode.SYNCED -> pregeneratedSlots.size - 1
        }
        pregeneratedSlots[index] = reward
    }
}
