package com.dawnshade.biteforce.bitecrates.feature.particle

import net.minecraft.server.level.ServerPlayer

abstract class AnimationAction {
    abstract fun tick(players: List<ServerPlayer>)
    open fun isComplete(): Boolean = true
    open fun reset() {}
    open fun length(): Int = 1
}