package com.dawnshade.biteforce.bitecrates.feature.particle.actions

import com.dawnshade.biteforce.bitecrates.feature.particle.AnimationAction
import net.minecraft.network.protocol.game.ClientboundBundlePacket
import net.minecraft.server.level.ServerPlayer

class ParticleAction(
    val packets: ClientboundBundlePacket,
): AnimationAction() {
    override fun tick(players: List<ServerPlayer>) {
        players.forEach { player ->
            player.connection.send(packets)
        }
    }
}