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

class BeamEffect(
    particle: ParticleOptions = ParticleTypes.ASH,
    speed: Int = 1,
    startDelay: Int = 0,
    endDelay: Int = 0,
    offset: Vec3? = null,
    val radius: Double = 1.0, 
    val points: Int = 20, 
    val height: Int = 20, 
    val spacing: Double = 1.0, 
    val upwards: Boolean = true,
    val rotation: Vec3? = null,
): ParticleEffect(EffectType.BEAM, particle, speed, startDelay, endDelay, offset) {
    override fun generateParticle(frame: Int, pos: Vec3): ParticleAction? {
        if (points <= 0 || height <= 0) return null

        val rx = Math.toRadians(rotation?.x ?: 0.0)
        val ry = Math.toRadians(rotation?.y ?: 0.0)
        val rz = Math.toRadians(rotation?.z ?: 0.0)

        val cosX = cos(rx)
        val sinX = sin(rx)
        val cosY = cos(ry)
        val sinY = sin(ry)
        val cosZ = cos(rz)
        val sinZ = sin(rz)

        val baseY = (offset?.y ?: 0.0) + pos.y
        val packets = ArrayList<ClientboundLevelParticlesPacket>(points * height)
        val baseAngle = (2.0 * PI * frame.toDouble() / points.toDouble())
        val localY = if (upwards) frame.toDouble() * spacing else -frame.toDouble() * spacing

        for (i in 0 until points) {
            val angle = baseAngle + (2.0 * PI * i.toDouble() / points.toDouble())

            val lx = cos(angle) * radius
            val lz = sin(angle) * radius

            val (rxX, rxY, rxZ) = rotatePoint(
                lx, localY, lz,
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

    override fun frameCount(): Int = height
}