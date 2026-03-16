package com.dawnshade.biteforce.bitecrates.config

class FailureOptions(
    val pushback: Double? = null,
    val sound: SoundOption? = null,
) {
    override fun toString(): String {
        return "FailureOptions(force=$pushback, sound=$sound)"
    }
}
