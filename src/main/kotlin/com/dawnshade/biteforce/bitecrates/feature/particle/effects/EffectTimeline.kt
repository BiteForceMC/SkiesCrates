package com.dawnshade.biteforce.bitecrates.feature.particle.effects

import com.dawnshade.biteforce.bitecrates.feature.particle.AnimationAction
import net.minecraft.server.level.ServerPlayer

class EffectTimeline(
    val actions: MutableList<AnimationAction>
) {
    constructor(): this(mutableListOf())

    private var currentIndex = 0

    fun isComplete() = actions.isEmpty() || (currentIndex >= actions.size && actions.all { it.isComplete()  })

    fun tick(players: List<ServerPlayer>) {
        if (currentIndex >= actions.size) return

        val currentAction = actions[currentIndex]
        currentAction.tick(players)

        if (currentAction.isComplete()) {
            currentIndex++
        }
    }

    fun reset() {
        currentIndex = 0
        for (action in actions) {
            action.reset()
        }
    }

    fun addAction(action: AnimationAction): EffectTimeline {
        actions.add(action)
        return this
    }
}