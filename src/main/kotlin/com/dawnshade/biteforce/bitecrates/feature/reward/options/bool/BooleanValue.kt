package com.dawnshade.biteforce.bitecrates.feature.reward.options.bool

class BooleanValue(val bool: Boolean) : BooleanOption {
    override fun getValue(): Boolean {
        return bool
    }
}