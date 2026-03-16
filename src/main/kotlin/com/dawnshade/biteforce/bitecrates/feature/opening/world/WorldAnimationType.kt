package com.dawnshade.biteforce.bitecrates.feature.opening.world

import com.dawnshade.biteforce.bitecrates.feature.opening.world.types.SimpleRollWorldAnimation
import com.dawnshade.biteforce.bitecrates.feature.opening.world.types.CarouselWorldAnimation

enum class WorldAnimationType(val identifier: String, val clazz: Class<*>) {
    SIMPLE_ROLL("simple_roll", SimpleRollWorldAnimation::class.java),
    CAROUSEL("carousel", CarouselWorldAnimation::class.java);

    companion object {
        fun valueOfAnyCase(name: String): WorldAnimationType? {
            for (type in entries) {
                if (name.equals(type.identifier, true)) return type
            }
            return null
        }
    }
}
