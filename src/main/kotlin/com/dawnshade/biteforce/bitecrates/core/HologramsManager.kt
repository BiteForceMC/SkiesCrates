package com.dawnshade.biteforce.bitecrates.core

import com.dawnshade.biteforce.bitecrates.core.BiteCrates
import com.dawnshade.biteforce.bitecrates.data.CrateInstance
import com.dawnshade.biteforce.bitecrates.integrations.holodisplays.CrateHologramData
import com.dawnshade.biteforce.bitecrates.mixin.ViewerHandlerAccessor
import com.dawnshade.biteforce.bitecrates.util.Utils
import dev.furq.holodisplays.api.HoloDisplaysAPI
import dev.furq.holodisplays.api.HoloDisplaysAPI.HologramBuilder
import dev.furq.holodisplays.handlers.ViewerHandler
import net.minecraft.server.level.ServerPlayer

object HologramsManager {
    private var hologramsAPI: HoloDisplaysAPI = HoloDisplaysAPI.get(BiteCrates.MOD_ID)
    private val holograms = mutableMapOf<String, CrateHologramData>()

    fun load() {
        unload()
        CratesManager.getAllInstances().forEach { instance ->
            loadCrateHologram(instance)
        }
    }

    fun loadCrateHologram(instance: CrateInstance) {
        val hologramConfig = instance.hologram ?: return
        val id = BiteCrates.asResource(instance.dimPos.hashCode().toString()).toString()

        hologramsAPI.createTextDisplay(
            id
        ) { builder ->
            builder.text(*hologramConfig.text.map {
                instance.crate.parsePlaceholders(it)
            }.toTypedArray())
            builder.scale(hologramConfig.scale.x, hologramConfig.scale.y, hologramConfig.scale.z)
            builder.rotation(hologramConfig.rotation.x, hologramConfig.rotation.y, hologramConfig.rotation.z)
            builder.billboardMode(hologramConfig.billboard.name)
            builder.shadow(hologramConfig.shadow)
            hologramConfig.background?.let {
                builder.backgroundColor(it.color, it.opacity)
            }
            builder.opacity(hologramConfig.opacity)
        }

        val builder: HologramBuilder = hologramsAPI.createHologramBuilder()
            
            .position(
                instance.dimPos.x + 0.5f + hologramConfig.offset.x,
                instance.dimPos.y + 0.5f + hologramConfig.offset.y,
                instance.dimPos.z + 0.5f + hologramConfig.offset.z
            )
            .world(instance.dimPos.dimension)
            .addDisplay(id)
            .updateRate(hologramConfig.updateRate)
            .viewRange(hologramConfig.viewDistance)

        if (!hologramsAPI.registerHologram(id, builder.build())) {
            Utils.printError("Failed to register hologram with ID: $id")
            return
        }
        holograms[id] = CrateHologramData(instance)
    }

    fun unload() {
        holograms.forEach { (id, _) ->
            hologramsAPI.unregisterDisplay(id)
            hologramsAPI.unregisterHologram(id)
        }
    }

    fun unloadCrateHologram(instance: CrateInstance) {
        val id = BiteCrates.asResource(instance.dimPos.hashCode().toString()).toString()
        hologramsAPI.unregisterDisplay(id)
        hologramsAPI.unregisterHologram(id)
        holograms.remove(id)
    }

    fun getHologramData(id: String): CrateHologramData? {
        return holograms[id]
    }

    fun hideHologramForPlayer(player: ServerPlayer, crateInstance: CrateInstance) {
        val id = BiteCrates.asResource(crateInstance.dimPos.hashCode().toString()).toString()
        val crateHologram = holograms[id] ?: return

        crateHologram.hiddenPlayers.add(player.uuid)
        (ViewerHandler as ViewerHandlerAccessor).invokeRemoveViewer(player, id)
    }

    fun showHologramForPlayer(player: ServerPlayer, crateInstance: CrateInstance) {
        val id = BiteCrates.asResource(crateInstance.dimPos.hashCode().toString()).toString()
        val crateHologram = holograms[id] ?: return

        crateHologram.hiddenPlayers.remove(player.uuid)
    }
}
