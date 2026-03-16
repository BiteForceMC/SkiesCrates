package com.dawnshade.biteforce.bitecrates.gui

import com.dawnshade.biteforce.bitecrates.data.Crate
import com.dawnshade.biteforce.bitecrates.feature.opening.inventory.InventoryOpeningAnimation
import com.dawnshade.biteforce.bitecrates.feature.opening.inventory.InventoryOpeningInstance
import com.dawnshade.biteforce.bitecrates.feature.opening.inventory.spinners.AnimatedSpinnerInstance
import com.dawnshade.biteforce.bitecrates.feature.opening.inventory.spinners.RewardSpinnerInstance
import com.dawnshade.biteforce.bitecrates.feature.reward.Reward
import com.dawnshade.biteforce.bitecrates.util.RandomCollection
import com.dawnshade.biteforce.bitecrates.util.TextUtils
import com.dawnshade.biteforce.bitecrates.util.Utils
import eu.pb4.sgui.api.gui.SimpleGui
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack

class CrateInventory(
    player: ServerPlayer,
    val opening: InventoryOpeningInstance
): SimpleGui(opening.animation.type.type, player, false) {
    private var isFinished = false
    private var closeTicks = 0

    private val cachedAnimatedPresets: MutableMap<String, RandomCollection<ItemStack>> = mutableMapOf()
    private var animatedSpinners: MutableMap<String, AnimatedSpinnerInstance> = mutableMapOf()

    private var cachedRewardStacks: MutableMap<String, ItemStack> = mutableMapOf()
    private var rewardSpinners: MutableMap<String, RewardSpinnerInstance> = mutableMapOf()

    private val userData = opening.chargeContext.userData

    private val crate: Crate = opening.crate
    private val animation: InventoryOpeningAnimation = opening.animation
    private var randomBag: RandomCollection<Reward> = opening.randomBag

    init {
        this.title = TextUtils.parseAllNative(player, opening.crate.parsePlaceholders(animation.title))

        animation.items.static.forEach { (id, item) ->
            item.slots.forEach { slot ->
                this.setSlot(slot, item.createItemStack(player))
            }
        }

        animation.presets.animations.forEach { (id, presetItems) ->
            val collection = RandomCollection<ItemStack>()
            presetItems.forEach { animatedItem ->
                val itemStack = animatedItem.createItemStack(player)
                collection.add(itemStack, animatedItem.weight.toDouble())
            }
            cachedAnimatedPresets[id] = collection
        }
        animation.items.animated.forEach { (id, spinningItem) ->
            if (spinningItem.preset.isEmpty()) return@forEach
            val bag = cachedAnimatedPresets[spinningItem.preset] ?: run {
                Utils.printError("Animated preset ${spinningItem.preset} not found for spinner $id")
                return@forEach
            }

            animatedSpinners[id] = AnimatedSpinnerInstance(spinningItem, bag).also { it.pregenerate() }
        }

        crate.rewards.forEach { (id, reward) ->
            cachedRewardStacks[id] = reward.getDisplayItem(player, reward.getPlaceholders(userData, crate))
        }
        val reservedRewardIds = mutableSetOf<String>()
        animation.items.rewards.forEach { (id, item) ->
            val spinner = RewardSpinnerInstance(item, randomBag, animation.winSlots).also {
                it.pregenerate()
            }

            val returnBag = spinner.validateRewards(crate, userData, reservedRewardIds)

            if (returnBag == null) {
                Utils.printError("No rewards were possible for spinner $id in crate ${crate.id} for player ${player.name.string}. Cancelling crate!")
                failOpening()
                return@forEach
            }

            randomBag = returnBag
            rewardSpinners[id] = spinner
        }
    }

    fun tick() {
        if (isFinished) {
            if (closeTicks++ >= animation.closeDelay) {
                this.close()
                return
            }
        } else {
            var allCompleted = true
            rewardSpinners.forEach { (_, spinner) ->
                if (spinner.isCompleted()) {
                    return@forEach
                }
                allCompleted = false
                spinner.tick(player, this)
            }

            if (allCompleted) {
                isFinished = true
                finishOpening()
            }
        }

        animatedSpinners.forEach { (_, spinner) ->
            if (spinner.isCompleted()) return@forEach
            spinner.tick(player, this)
        }
    }

    fun updateRewardSlot(slot: Int, reward: Reward) {
        this.setSlot(slot, cachedRewardStacks[reward.id] ?: reward.getDisplayItem(player, reward.getPlaceholders(userData, crate)))
    }

    override fun onClose() {
        if (!isFinished) {
            if (animation.skippable) {
                isFinished = true
                finishOpening()
            } else {
                this.open()
                return
            }
        }

        opening.stop()
    }

    private fun finishOpening() {
        val grantedRewards = mutableListOf<Reward>()
        rewardSpinners.forEach { (_, data) ->
            val rewards = data.giveRewards(player, crate) ?: run {
                failOpening()
                return
            }
            grantedRewards += rewards
        }
        if (!opening.chargeContext.complete(grantedRewards)) {
            return
        }
    }

    private fun failOpening() {
        opening.chargeContext.refund()
        player.sendMessage(Component.text("An error occurred while opening the crate. Your costs were refunded.", NamedTextColor.RED))
        isFinished = true
        close()
    }
}
