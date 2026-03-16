package com.dawnshade.biteforce.bitecrates.feature.reward.options.bool

class BooleanChance(
    val chance: Float
): BooleanOption {
    override fun getValue(): Boolean {
        return Math.random() < chance
    }
}