package com.dawnshade.biteforce.bitecrates.feature.opening

import com.dawnshade.biteforce.bitecrates.data.Crate
import com.dawnshade.biteforce.bitecrates.core.OpeningManager
import net.minecraft.server.level.ServerPlayer

abstract class OpeningInstance(
    val player: ServerPlayer,
    val crate: Crate,
    val chargeContext: OpenChargeContext,
) {
    abstract fun setup()

    abstract fun tick()

    open fun stop() {
        destroy()
    }

    open fun destroy() {
        OpeningManager.removeInstance(player.uuid)
    }
}
