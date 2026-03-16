package com.dawnshade.biteforce.bitecrates.feature.particle

import com.dawnshade.biteforce.bitecrates.feature.particle.effects.EffectTimeline
import net.minecraft.server.level.ServerPlayer

class ParticleAnimation {
    private val timelines = mutableListOf<EffectTimeline>() 
    private var mode = AnimationMode.SEQUENTIAL
    private var distance = 50.0
    private var currentIndex = 0

    fun tick(players: List<ServerPlayer>) {
        if (timelines.isEmpty()) return

        when (mode) {
            AnimationMode.SEQUENTIAL -> {
                val currentTimeline = timelines[currentIndex]
                currentTimeline.tick(players)

                if (currentTimeline.isComplete()) {
                    if (currentIndex >= timelines.size - 1) {
                        reset()
                    } else {
                        currentIndex++
                    }
                }
            }
            AnimationMode.CONCURRENT -> {
                for (timeline in timelines) {
                    timeline.tick(players)
                }

                if (timelines.all { it.isComplete() }) {
                    reset()
                }
            }
        }
    }

    fun reset() {
        currentIndex = 0
        for (timeline in timelines) {
            timeline.reset()
        }
    }

    fun addTimeline(timeline: EffectTimeline): ParticleAnimation {
        timelines.add(timeline)
        return this
    }

    fun setMode(mode: AnimationMode): ParticleAnimation {
        this.mode = mode
        return this
    }

    fun setDistance(distance: Double): ParticleAnimation {
        this.distance = distance
        return this
    }

    fun getDistance(): Double {
        return distance
    }
}