package com.dawnshade.biteforce.bitecrates.feature.opening

import com.dawnshade.biteforce.bitecrates.core.BiteCrates
import com.dawnshade.biteforce.bitecrates.config.Lang
import com.dawnshade.biteforce.bitecrates.data.Crate
import com.dawnshade.biteforce.bitecrates.feature.key.Key
import com.dawnshade.biteforce.bitecrates.feature.reward.Reward
import com.dawnshade.biteforce.bitecrates.state.userdata.UserData
import com.dawnshade.biteforce.bitecrates.economy.IEconomyService
import com.dawnshade.biteforce.bitecrates.util.TextUtils
import com.dawnshade.biteforce.bitecrates.util.Utils
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack

class OpenChargeContext(
    val player: ServerPlayer,
    val crate: Crate,
    val userData: UserData,
) {
    private val refundedVirtualKeys = linkedMapOf<Key, Int>()
    private val refundedItems = mutableListOf<ItemStack>()
    private val refundedBalances = mutableListOf<BalanceCharge>()
    private var persisted = false
    private var settled = false

    fun recordVirtualKeyRemoval(key: Key, amount: Int) {
        refundedVirtualKeys.merge(key, amount, Int::plus)
    }

    fun recordItemRemoval(itemStack: ItemStack) {
        refundedItems += itemStack.copy()
    }

    fun recordBalanceCharge(service: IEconomyService, amount: Double, currency: String) {
        refundedBalances += BalanceCharge(service, amount, currency)
    }

    fun persistDeductions(): Boolean {
        if (persisted) {
            return true
        }
        val saved = BiteCrates.INSTANCE.storage.saveUser(userData)
        if (!saved) {
            Utils.printError("Failed to persist crate deductions for ${player.name.string} on crate ${crate.id}.")
            Lang.ERROR_STORAGE.forEach {
                player.sendMessage(TextUtils.toNative(it))
            }
            return false
        }
        persisted = true
        return true
    }

    fun complete(rewards: Collection<Reward>): Boolean {
        if (settled) {
            return true
        }
        userData.addCrateUse(crate)
        rewards.filter { reward -> reward.getPlayerLimit() > 0 }.forEach { reward ->
            userData.addRewardUse(crate, reward)
        }
        val saved = BiteCrates.INSTANCE.storage.saveUser(userData)
        if (!saved) {
            Utils.printError("Failed to persist crate completion for ${player.name.string} on crate ${crate.id}.")
            Lang.ERROR_STORAGE.forEach {
                player.sendMessage(TextUtils.toNative(it))
            }
            return false
        }
        settled = true
        return true
    }

    fun refund(): Boolean {
        if (settled) {
            return true
        }
        refundedVirtualKeys.forEach { (key, amount) ->
            userData.addKeys(key, amount)
        }
        refundedItems.forEach { itemStack ->
            player.inventory.placeItemBackInInventory(itemStack)
        }
        refundedBalances.forEach { charge ->
            if (!charge.service.deposit(player, charge.amount, charge.currency)) {
                Utils.printError("Failed to refund ${charge.amount} ${charge.currency} for ${player.name.string} on crate ${crate.id}.")
            }
        }
        if (!persisted && refundedVirtualKeys.isEmpty()) {
            settled = true
            return true
        }
        val saved = BiteCrates.INSTANCE.storage.saveUser(userData)
        if (!saved) {
            Utils.printError("Failed to persist crate refund for ${player.name.string} on crate ${crate.id}.")
            Lang.ERROR_STORAGE.forEach {
                player.sendMessage(TextUtils.toNative(it))
            }
            return false
        }
        settled = true
        return true
    }

    private data class BalanceCharge(
        val service: IEconomyService,
        val amount: Double,
        val currency: String,
    )
}
