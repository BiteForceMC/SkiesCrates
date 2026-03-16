package com.dawnshade.biteforce.bitecrates.config.block

class ModelOptions(
    var id: String = "",
    val rotation: Float = 0f,
    val offset: HologramOptions.XYZOption = HologramOptions.XYZOption(),
    val scale: Float = 1.0f,
    val hitbox: HitboxOptions = HitboxOptions(),
    val animations: Animations = Animations()
) {
    class HitboxOptions(
        val width: Float = 1.0f,
        val height: Float = 1.0f,
        val offset: HologramOptions.XYZOption = HologramOptions.XYZOption(),
    )

    class Animations(
        val idle: String? = null,
        val idleInterval: Int = 0,
        val timeline: List<TimelineEntry> = emptyList()
    ) {
        class TimelineEntry(
            val time: Int = 0,
            val animation: String = ""
        )
    }
}
