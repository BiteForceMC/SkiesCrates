package com.dawnshade.biteforce.bitecrates.feature.reward

class RewardLimits(
    val player: Limit? = null,
) {
    class Limit(
        val amount: Int = 0,
        val cooldown: Long = 0,
    ) {
        override fun toString(): String {
            return "Limit(amount=$amount, cooldown=$cooldown)"
        }
    }

    override fun toString(): String {
        return "RewardLimits(player=$player)"
    }
}