package com.dawnshade.biteforce.bitecrates.economy

import com.dawnshade.biteforce.bitecrates.util.Utils
import net.minecraft.server.level.ServerPlayer
import kotlin.reflect.full.primaryConstructor

interface IEconomyService {
    fun balance(player: ServerPlayer, currency: String = "") : Double
    fun withdraw(player: ServerPlayer, amount: Double, currency: String = "") : Boolean
    fun deposit(player: ServerPlayer, amount: Double, currency: String = "") : Boolean
    fun set(player: ServerPlayer, amount: Double, currency: String = "") : Boolean

    companion object {
        private fun getEconomyService(economyType: InternalEconomyTypes) : IEconomyService? {
            if (!economyType.isModPresent()) return null

            return try {
                economyType.clazz.kotlin.primaryConstructor!!.call()
            } catch (ex: Exception) {
                Utils.printError("There was an exception while initializing the Economy Service: ${economyType}. Is it loaded?")
                com.dawnshade.biteforce.bitecrates.core.BiteCrates.LOGGER.error("Failed to initialize economy service {}", economyType, ex)
                null
            }
        }
    }
}
