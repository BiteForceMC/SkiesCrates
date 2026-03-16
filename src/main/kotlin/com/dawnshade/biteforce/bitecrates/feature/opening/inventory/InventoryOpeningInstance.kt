package com.dawnshade.biteforce.bitecrates.feature.opening.inventory

import com.dawnshade.biteforce.bitecrates.data.Crate
import com.dawnshade.biteforce.bitecrates.feature.opening.OpeningInstance
import com.dawnshade.biteforce.bitecrates.feature.opening.OpenChargeContext
import com.dawnshade.biteforce.bitecrates.feature.reward.Reward
import com.dawnshade.biteforce.bitecrates.gui.CrateInventory
import com.dawnshade.biteforce.bitecrates.util.RandomCollection
import net.minecraft.server.level.ServerPlayer

class InventoryOpeningInstance(
    player: ServerPlayer,
    crate: Crate,
    chargeContext: OpenChargeContext,
    val animation: InventoryOpeningAnimation,
    val randomBag: RandomCollection<Reward>,
): OpeningInstance(player, crate, chargeContext) {
    private val gui = CrateInventory(player, this)

    override fun setup() {
        gui.open()
    }

    override fun tick() {
        gui.tick()
    }
}
