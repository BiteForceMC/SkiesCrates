package com.dawnshade.biteforce.bitecrates.core

import com.dawnshade.biteforce.bitecrates.config.ConfigManager
import com.dawnshade.biteforce.bitecrates.data.DimensionalBlockPos
import com.dawnshade.biteforce.bitecrates.feature.opening.OpeningAnimation
import com.dawnshade.biteforce.bitecrates.feature.opening.OpeningInstance
import com.dawnshade.biteforce.bitecrates.util.Utils
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object OpeningManager {
    private val activeInstances: MutableMap<UUID, OpeningInstance> = ConcurrentHashMap()
    private val activeWorldCrates: MutableSet<DimensionalBlockPos> = ConcurrentHashMap.newKeySet()
    private val animations: MutableMap<String, OpeningAnimation> = mutableMapOf()

    fun load() {
        animations.clear()
        ConfigManager.OPENINGS_INVENTORY.forEach { (id, animation) ->
            registerAnimation(id, animation)
        }
        ConfigManager.OPENINGS_WORLD.forEach { (id, animation) ->
            registerAnimation(id, animation)
        }

        Utils.printInfo("Registered ${animations.size} opening animations!")
    }

    fun tick() {
        for ((_, instance) in activeInstances) {
            instance.tick()
        }
    }

    fun addInstance(playerId: UUID, instance: OpeningInstance) {
        activeInstances[playerId] = instance
    }

    fun getInstance(playerId: UUID): OpeningInstance? {
        return activeInstances[playerId]
    }

    fun removeInstance(playerId: UUID) {
        activeInstances.remove(playerId)
    }

    fun stopAll() {
        val instances = activeInstances.values.toList()
        activeInstances.clear()
        activeWorldCrates.clear()
        instances.forEach { instance ->
            runCatching { instance.stop() }
        }
    }

    fun tryLockWorldCrate(position: DimensionalBlockPos): Boolean {
        return activeWorldCrates.add(position)
    }

    fun unlockWorldCrate(position: DimensionalBlockPos) {
        activeWorldCrates.remove(position)
    }

    fun registerAnimation(id: String, animation: OpeningAnimation) {
        if (animations.containsKey(id)) {
            Utils.printError("Duplicate opening animation ID found: $id. Skipping...")
            return
        }
        animations[id] = animation
    }

    fun getAnimation(id: String): OpeningAnimation? {
        return animations[id]
    }
}
