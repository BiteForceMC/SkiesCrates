package com.dawnshade.biteforce.bitecrates.feature.reward.options.int

class IntValue(
    val int: Int
): IntOption {
    override fun getValue(): Int {
        return int
    }
}