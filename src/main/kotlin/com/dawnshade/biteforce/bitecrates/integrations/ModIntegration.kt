package com.dawnshade.biteforce.bitecrates.integrations

import com.dawnshade.biteforce.bitecrates.integrations.bil.BILIntegration
import com.dawnshade.biteforce.bitecrates.integrations.holodisplays.HoloDisplaysIntegration
import net.fabricmc.loader.api.FabricLoader

enum class ModIntegration(val modId: String, val clazz: Class<out IntegratedMod>) {
    HOLODISPLAYS("holodisplays", HoloDisplaysIntegration::class.java),
    FLAN("flan", FlanIntegration::class.java),
    BIL("bil", BILIntegration::class.java);

    fun isModLoaded(): Boolean {
        return FabricLoader.getInstance().isModLoaded(modId)
    }

    companion object {
        private val enabledIntegrations: Map<ModIntegration, IntegratedMod> by lazy {
            entries.filter { it.isModLoaded() }.mapNotNull {
                try {
                    it to it.clazz.getDeclaredConstructor().newInstance()
                } catch (_: Exception) {
                    null
                }
            }.toMap()
        }

        fun onInit() {
            enabledIntegrations.forEach { (_, integration) -> integration.onInit() }
        }

        fun onServerStarting() {
            enabledIntegrations.forEach { (_, integration) -> integration.onServerStarting() }
        }

        fun onServerStarted() {
            enabledIntegrations.forEach { (_, integration) -> integration.onServerStarted() }
        }

        fun onServerShutdown() {
            enabledIntegrations.forEach { (_, integration) -> integration.onServerShutdown() }
        }
    }

    fun getIntegration(): IntegratedMod? {
        return enabledIntegrations.entries.firstOrNull { it.key == this }?.value
    }
}