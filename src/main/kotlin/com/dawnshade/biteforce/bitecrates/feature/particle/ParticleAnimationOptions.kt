package com.dawnshade.biteforce.bitecrates.feature.particle

import com.dawnshade.biteforce.bitecrates.data.CrateInstance
import com.dawnshade.biteforce.bitecrates.feature.particle.effects.ParticleEffect

class ParticleAnimationOptions(
    val mode: AnimationMode,
    val distance: Double,
    val effects: List<ParticleEffect>
) {
    fun generateAnimation(instance: CrateInstance): ParticleAnimation {
        val animation = ParticleAnimation()

        animation.setMode(mode)
        animation.setDistance(distance)

        for (effect in effects) {
            animation.addTimeline(effect.generateTimeline(instance))
        }

        return animation
    }
}