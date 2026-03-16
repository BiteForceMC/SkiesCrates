package com.dawnshade.biteforce.bitecrates.feature.particle.effects

import com.google.gson.*
import com.google.gson.annotations.SerializedName
import com.dawnshade.biteforce.bitecrates.data.CrateInstance
import com.dawnshade.biteforce.bitecrates.feature.particle.actions.DelayAction
import com.dawnshade.biteforce.bitecrates.feature.particle.actions.ParticleAction
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.world.phys.Vec3
import java.lang.reflect.Type

abstract class ParticleEffect(
    val type: EffectType,
    val particle: ParticleOptions,
    var speed: Int = 1, 
    @SerializedName("start_delay")
    var startDelay: Int = 0,
    @SerializedName("end_delay")
    var endDelay: Int = 0,
    val offset: Vec3? = null,
) {
    fun generateTimeline(instance: CrateInstance): EffectTimeline {
        val timeline = EffectTimeline()

        if (startDelay > 0) {
            timeline.addAction(DelayAction(startDelay))
        }

        val ticksPerFrame = speed.coerceAtLeast(1)

        for (i in 0 until frameCount()) {
            generateParticle(
                i,
                instance.pos.bottomCenter
            )?.let { particle ->
                timeline.addAction(particle)

                
                if (ticksPerFrame > 1 && i < frameCount() - 1) {
                    timeline.addAction(DelayAction(ticksPerFrame))
                }
            }
        }

        if (endDelay > 0) {
            timeline.addAction(DelayAction(endDelay))
        }

        return timeline
    }

    abstract fun generateParticle(frame: Int, pos: Vec3): ParticleAction?

    abstract fun frameCount(): Int

    protected fun rotatePoint(
        x: Double, y: Double, z: Double,
        cosX: Double, sinX: Double,
        cosY: Double, sinY: Double,
        cosZ: Double, sinZ: Double
    ): Triple<Double, Double, Double> {
        val y1 = y * cosX - z * sinX
        val z1 = y * sinX + z * cosX

        val x2 = x * cosY + z1 * sinY
        val z2 = -x * sinY + z1 * cosY

        val x3 = x2 * cosZ - y1 * sinZ
        val y3 = x2 * sinZ + y1 * cosZ

        return Triple(x3, y3, z2)
    }

    internal class Adapter : JsonSerializer<ParticleEffect>, JsonDeserializer<ParticleEffect> {
        override fun serialize(src: ParticleEffect, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            return context.serialize(src, src::class.java)
        }

        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ParticleEffect {
            val jsonObject: JsonObject = json.getAsJsonObject()
            val value = jsonObject.get("type").asString
            val type: EffectType? = EffectType.valueOfAnyCase(value)
            return try {
                context.deserialize(json, type!!.clazz)
            } catch (e: NullPointerException) {
                throw JsonParseException("Could not deserialize effect type: $value", e)
            }
        }
    }
}