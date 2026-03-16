package com.dawnshade.biteforce.bitecrates.feature.opening.world

import com.dawnshade.biteforce.bitecrates.data.Crate
import com.dawnshade.biteforce.bitecrates.data.CrateInstance
import com.dawnshade.biteforce.bitecrates.core.OpeningManager
import com.dawnshade.biteforce.bitecrates.feature.opening.OpeningInstance
import com.dawnshade.biteforce.bitecrates.feature.opening.OpenChargeContext
import com.dawnshade.biteforce.bitecrates.feature.reward.Reward
import com.dawnshade.biteforce.bitecrates.util.RandomCollection
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3

class WorldOpeningInstance(
    player: ServerPlayer,
    crate: Crate,
    chargeContext: OpenChargeContext,
    val instance: CrateInstance,
    val animation: WorldOpeningAnimation,
    val randomBag: RandomCollection<Reward>,
): OpeningInstance(player, crate, chargeContext) {
    val initialPlayerPosition: Vec3 = player.position()

    override fun tick() {
        animation.tick(this)
    }

    override fun setup() {
        instance.bilData?.startOpeningTimeline()
        animation.setup(this)
    }

    override fun stop() {
        super.stop()
        OpeningManager.unlockWorldCrate(instance.dimPos)
        instance.bilData?.stopOpeningTimeline()
        animation.stop(this)
    }
}
