package com.pokeskies.skiescrates.config.block

import net.minecraft.world.phys.Vec3

class ModelOptions(
    var id: String = "",
    val rotation: Float = 0f,
    val offset: Vec3 = Vec3(0.0, 0.0, 0.0),
    val scale: Float = 1.0f,
    val hitbox: HitboxOptions = HitboxOptions(),
    val animations: Animations = Animations()
) {
    class HitboxOptions(
        val width: Float = 1.0f,
        val height: Float = 1.0f,
        val offset: Vec3 = Vec3(0.0, 0.0, 0.0),
    )

    class Animations(
        val idle: String? = null,
        val open: String? = null,
        val close: String? = null,
    )
}
