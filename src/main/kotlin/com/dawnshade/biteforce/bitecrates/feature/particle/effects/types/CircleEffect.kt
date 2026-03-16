package com.dawnshade.biteforce.bitecrates.feature.particle.effects.types

import com.dawnshade.biteforce.bitecrates.feature.particle.actions.ParticleAction
import com.dawnshade.biteforce.bitecrates.feature.particle.effects.EffectType
import com.dawnshade.biteforce.bitecrates.feature.particle.effects.ParticleEffect
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.protocol.game.ClientboundBundlePacket
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.world.phys.Vec3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


class CircleEffect(
    particle: ParticleOptions = ParticleTypes.ASH,
    speed: Int = 1,
    startDelay: Int = 0,
    endDelay: Int = 0,
    offset: Vec3? = null,
    val radius: Double = 1.0, 
    val points: Int = 20, 
    val phase: Double = 0.0, 
    val clockwise: Boolean = true,
    val strands: Int = 1, 
    val rotation: Vec3? = null,
): ParticleEffect(EffectType.CIRCLE, particle, speed, startDelay, endDelay, offset) {
    override fun generateParticle(frame: Int, pos: Vec3): ParticleAction? {
        if (points <= 0) return null

        val direction = if (clockwise) 1.0 else -1.0
        val baseY = (offset?.y ?: 0.0) + pos.y
        val strandCount = strands.coerceAtLeast(1)

        val rx = Math.toRadians(rotation?.x ?: 0.0)
        val ry = Math.toRadians(rotation?.y ?: 0.0)
        val rz = Math.toRadians(rotation?.z ?: 0.0)

        val cosX = cos(rx)
        val sinX = sin(rx)
        val cosY = cos(ry)
        val sinY = sin(ry)
        val cosZ = cos(rz)
        val sinZ = sin(rz)

        val packets = ArrayList<ClientboundLevelParticlesPacket>(strandCount)

        for (i in 0 until strandCount) {
            val baseAngle = (2.0 * PI * frame / points) * direction
            val strandOffset = 2.0 * PI * i / strandCount
            val angle = baseAngle + strandOffset + phase

            val lx = cos(angle) * radius
            val lz = sin(angle) * radius

            val (rxX, rxY, rxZ) = rotatePoint(
                lx, 0.0, lz,
                cosX, sinX,
                cosY, sinY,
                cosZ, sinZ
            )

            packets.add(ClientboundLevelParticlesPacket(
                particle,
                false,
                (offset?.x ?: 0.0) + pos.x + rxX,
                baseY + rxY,
                (offset?.z ?: 0.0) + pos.z + rxZ,
                0.0f, 0.0f, 0.0f,
                0.0f,
                1
            ))
        }

        return ParticleAction(ClientboundBundlePacket(packets))
    }

    override fun frameCount(): Int = points
}