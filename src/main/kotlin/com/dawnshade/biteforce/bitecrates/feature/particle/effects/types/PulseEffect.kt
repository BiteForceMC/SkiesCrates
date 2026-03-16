package com.dawnshade.biteforce.bitecrates.feature.particle.effects.types

import com.google.gson.annotations.SerializedName
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
import kotlin.math.roundToInt
import kotlin.math.sin

class PulseEffect(
    particle: ParticleOptions = ParticleTypes.ASH,
    speed: Int = 1,
    startDelay: Int = 0,
    endDelay: Int = 0,
    offset: Vec3? = null,
    @SerializedName("start_radius")
    val startRadius: Double = 1.0, 
    @SerializedName("end_radius")
    val endRadius: Double = 1.0, 
    @SerializedName("start_points")
    val startPoints: Int = 20, 
    @SerializedName("end_points")
    val endPoints: Int = 20, 
    val rings: Int = 5, 
    val outwards: Boolean = true, 
    val rotation: Vec3? = null,
): ParticleEffect(EffectType.PULSE, particle, speed, startDelay, endDelay, offset) {
    override fun generateParticle(frame: Int, pos: Vec3): ParticleAction? {
        if (rings <= 0) return null

        val tNorm = if (rings > 1) frame.toDouble() / (rings - 1).toDouble() else 0.0
        val t = if (outwards) tNorm else 1.0 - tNorm

        val radius = startRadius + (endRadius - startRadius) * t
        val pointsDouble = startPoints + (endPoints - startPoints) * t
        val points = pointsDouble.roundToInt().coerceAtLeast(1)

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
        val packets = ArrayList<ClientboundLevelParticlesPacket>(points)
        val baseAngle = (2.0 * PI * frame.toDouble() / points.toDouble())

        for (i in 0 until points) {
            val angle = baseAngle + (2.0 * PI * i.toDouble() / points.toDouble())

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

    override fun frameCount(): Int = rings
}