package com.dawnshade.biteforce.bitecrates.core

import com.dawnshade.biteforce.bitecrates.config.ConfigManager
import com.dawnshade.biteforce.bitecrates.config.Lang
import com.dawnshade.biteforce.bitecrates.data.DimensionalBlockPos
import com.dawnshade.biteforce.bitecrates.feature.opening.OpeningAnimation
import com.dawnshade.biteforce.bitecrates.feature.opening.OpeningInstance
import com.dawnshade.biteforce.bitecrates.feature.opening.world.WorldOpeningInstance
import com.dawnshade.biteforce.bitecrates.util.TextUtils
import com.dawnshade.biteforce.bitecrates.util.Utils
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object OpeningManager {
    private val activeInstances: MutableMap<UUID, OpeningInstance> = ConcurrentHashMap()
    private val activeWorldCrates: MutableMap<DimensionalBlockPos, Int> = ConcurrentHashMap()
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
        for (instance in activeInstances.values.toList()) {
            runCatching { instance.tick() }.onFailure { ex ->
                Utils.printError("Failed to tick opening for ${instance.player.name.string} on crate ${instance.crate.id}. Closing opening and refunding costs.")
                BiteCrates.LOGGER.error("Failed to tick opening for {} on crate {}", instance.player.name.string, instance.crate.id, ex)

                instance.chargeContext.refund()
                Lang.ERROR_OPENING.forEach {
                    instance.player.sendMessage(TextUtils.parseAllNative(instance.player, instance.crate.parsePlaceholders(it)))
                }

                removeInstance(instance.player.uuid)
                runCatching { instance.stop() }.onFailure { stopEx ->
                    BiteCrates.LOGGER.error("Failed to stop opening for {} on crate {}", instance.player.name.string, instance.crate.id, stopEx)
                    if (instance is WorldOpeningInstance) {
                        unlockWorldCrate(instance.instance.dimPos)
                    }
                }
            }
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
        val maxOpeners = ConfigManager.CONFIG.maxOpenersPerCrate
        if (maxOpeners <= 0) {
            return true
        }

        synchronized(activeWorldCrates) {
            val currentOpeners = activeWorldCrates[position] ?: 0
            if (currentOpeners >= maxOpeners) {
                return false
            }
            activeWorldCrates[position] = currentOpeners + 1
            return true
        }
    }

    fun unlockWorldCrate(position: DimensionalBlockPos) {
        if (ConfigManager.CONFIG.maxOpenersPerCrate <= 0) {
            return
        }

        synchronized(activeWorldCrates) {
            val currentOpeners = activeWorldCrates[position] ?: return
            if (currentOpeners <= 1) {
                activeWorldCrates.remove(position)
            } else {
                activeWorldCrates[position] = currentOpeners - 1
            }
        }
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
