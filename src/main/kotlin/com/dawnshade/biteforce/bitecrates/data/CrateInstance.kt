package com.dawnshade.biteforce.bitecrates.data

import com.dawnshade.biteforce.bitecrates.core.BiteCrates
import com.dawnshade.biteforce.bitecrates.config.block.HologramOptions
import com.dawnshade.biteforce.bitecrates.config.block.ModelOptions
import com.dawnshade.biteforce.bitecrates.config.block.PreviewDisplayOptions
import com.dawnshade.biteforce.bitecrates.feature.particle.ParticleAnimation
import com.dawnshade.biteforce.bitecrates.feature.particle.ParticleAnimationOptions
import com.dawnshade.biteforce.bitecrates.integrations.ModIntegration
import com.dawnshade.biteforce.bitecrates.integrations.bil.BILCrateData
import com.dawnshade.biteforce.bitecrates.core.HologramsManager
import com.dawnshade.biteforce.bitecrates.util.Utils
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer

class CrateInstance(
    val crate: Crate,
    val level: ServerLevel,
    val pos: BlockPos,
    val dimPos: DimensionalBlockPos,
    val model: ModelOptions? = null,
    val hologram: HologramOptions? = null,
    val previewDisplay: PreviewDisplayOptions? = null,
    val particles: ParticleAnimationOptions? = null,
) {
    private var particleAnimation: ParticleAnimation? = null
    var bilData: BILCrateData? = null

    private var nearPlayers = mutableSetOf<ServerPlayer>()
    private var ticks = 0

    init {
        particleAnimation = particles?.generateAnimation(this)

        model?.let { modelOptions ->
            if (!ModIntegration.BIL.isModLoaded()) {
                Utils.printError("The crate '${crate.id}' is using a crate model but the Blockbench Import Library mod is not installed!")
                return@let
            }

            if (!level.isLoaded(pos)) {
                return@let
            }

            val chunk = try {
                level.getChunkAt(pos)
            } catch (e: Exception) {
                Utils.printError("Error while attaching BIL Crate at chunk ${pos}!")
                BiteCrates.LOGGER.error("Failed to attach BIL crate at {}", pos, e)
                return@let
            }

            bilData = BILCrateData.create(this, chunk, modelOptions)
        }
    }

    fun destroy() {
        bilData?.holder?.destroy()

        if (FabricLoader.getInstance().isModLoaded("holodisplays")) {
            HologramsManager.unloadCrateHologram(this)
        }
    }

    fun tick() {
        bilData?.tick()

        particleAnimation?.let { particle ->
            BiteCrates.INSTANCE.runOnParticleThread {
                if (ticks++ >= 20) {
                    ticks = 0
                    nearPlayers = level.players().filter { p ->
                        p.distanceToSqr(pos.bottomCenter) < particle.getDistance()
                    }.toMutableSet()
                }

                particle.tick(nearPlayers.toList())
            }
        }
    }
}
