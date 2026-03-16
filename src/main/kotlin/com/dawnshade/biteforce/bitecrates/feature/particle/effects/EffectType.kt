package com.dawnshade.biteforce.bitecrates.feature.particle.effects

import com.dawnshade.biteforce.bitecrates.feature.particle.effects.types.BeamEffect
import com.dawnshade.biteforce.bitecrates.feature.particle.effects.types.CircleEffect
import com.dawnshade.biteforce.bitecrates.feature.particle.effects.types.PulseEffect
import com.dawnshade.biteforce.bitecrates.feature.particle.effects.types.SpiralEffect

enum class EffectType(val identifier: String, val clazz: Class<*>) {
    SPIRAL("spiral", SpiralEffect::class.java),
    CIRCLE("circle", CircleEffect::class.java),
    BEAM("beam", BeamEffect::class.java),
    PULSE("pulse", PulseEffect::class.java),
    




    ;

    companion object {
        fun valueOfAnyCase(name: String): EffectType? {
            for (type in entries) {
                if (name.equals(type.identifier, true)) return type
            }
            return null
        }
    }
}