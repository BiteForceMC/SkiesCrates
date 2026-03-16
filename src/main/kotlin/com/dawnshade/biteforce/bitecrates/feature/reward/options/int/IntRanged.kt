package com.dawnshade.biteforce.bitecrates.feature.reward.options.int

class IntRanged(
    val min: Int,
    val max: Int,
): IntOption {
    override fun getValue(): Int {
        return (min..max).random()
    }
}