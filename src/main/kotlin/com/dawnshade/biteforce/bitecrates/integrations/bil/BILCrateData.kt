package com.dawnshade.biteforce.bitecrates.integrations.bil

import com.dawnshade.biteforce.bitecrates.core.BiteCrates.Companion.asyncScope
import com.dawnshade.biteforce.bitecrates.config.block.ModelOptions
import com.dawnshade.biteforce.bitecrates.data.CrateInstance
import com.dawnshade.biteforce.bitecrates.data.CrateOpenData
import com.dawnshade.biteforce.bitecrates.integrations.ModIntegration
import com.dawnshade.biteforce.bitecrates.core.CratesManager.openCrate
import com.dawnshade.biteforce.bitecrates.core.CratesManager.previewCrate
import com.dawnshade.biteforce.bitecrates.util.Utils
import de.tomalbrc.bil.core.holder.positioned.PositionedHolder
import eu.pb4.polymer.virtualentity.api.attachment.ChunkAttachment
import eu.pb4.polymer.virtualentity.api.elements.InteractionElement
import eu.pb4.polymer.virtualentity.api.elements.VirtualElement.InteractionHandler
import kotlinx.coroutines.launch
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.phys.Vec3

class BILCrateData(
    var holder: PositionedHolder,
    private val modelOptions: ModelOptions
) {
    private var activeTimeline: ModelAnimationTimeline? = null
    private var idleTicks = 0

    fun isAttached(): Boolean {
        return holder.attachment != null
    }

    fun tick() {
        if (activeTimeline != null) {
            activeTimeline?.tick(holder)
            return
        }

        val idleAnimation = modelOptions.animations.idle
        val idleInterval = modelOptions.animations.idleInterval
        if (idleAnimation.isNullOrBlank() || idleInterval <= 0) {
            return
        }

        idleTicks++
        if (idleTicks >= idleInterval) {
            holder.animator.playAnimation(idleAnimation)
            idleTicks = 0
        }
    }

    fun startOpeningTimeline() {
        val entries = modelOptions.animations.timeline
            .filter { it.animation.isNotBlank() && it.time >= 0 }
            .sortedBy { it.time }
        idleTicks = 0
        if (entries.isEmpty()) {
            return
        }

        activeTimeline = ModelAnimationTimeline(entries, modelOptions.animations.idle)
        activeTimeline?.start(holder)
    }

    fun stopOpeningTimeline() {
        activeTimeline?.stop(holder)
        activeTimeline = null
        idleTicks = 0
    }

    companion object {
        fun create(instance: CrateInstance, chunk: LevelChunk?, modelOptions: ModelOptions): BILCrateData? {
            val integration = ModIntegration.BIL.getIntegration() as? BILIntegration ?: run {
                Utils.printError("BIL Integration is not initialized!")
                return null
            }

            val model = integration.getModel(modelOptions.id) ?: run {
                Utils.printError("The crate '${instance.crate.id}' is using a model '${modelOptions.id}' which could not be found!")
                return null
            }

            val holder = CrateModelHolder(
                instance.level,
                instance.pos.bottomCenter,
                model,
                modelOptions.rotation,
                modelOptions.offset.toVec3()
            )
            holder.scale = modelOptions.scale
            modelOptions.animations.idle?.let { holder.animator.playAnimation(it) }
            val element = InteractionElement()
            element.setSize(modelOptions.hitbox.width, modelOptions.hitbox.height)
            element.offset = modelOptions.hitbox.offset.toVec3()
            element.setHandler(object : InteractionHandler {
                override fun interactAt(player: ServerPlayer, hand: InteractionHand, pos: Vec3) {
                    asyncScope.launch {
                        openCrate(player, instance.crate, CrateOpenData(instance.dimPos, null), false)
                    }
                }

                override fun attack(player: ServerPlayer) {
                    previewCrate(player, instance.crate)
                }
            })
            holder.addElement(element)

            val pos = Vec3.atCenterOf(instance.pos)
            if (chunk != null) {
                ChunkAttachment(
                    holder,
                    chunk,
                    pos,
                    true
                )
            }

            return BILCrateData(holder, modelOptions)
        }
    }

    private class ModelAnimationTimeline(
        private val entries: List<ModelOptions.Animations.TimelineEntry>,
        private val idleAnimation: String?
    ) {
        private var elapsedTicks = 0
        private var nextIndex = 0

        fun start(holder: PositionedHolder) {
            playScheduledAnimations(holder)
        }

        fun tick(holder: PositionedHolder) {
            elapsedTicks++
            playScheduledAnimations(holder)
        }

        fun stop(holder: PositionedHolder) {
            if (!idleAnimation.isNullOrBlank()) {
                holder.animator.playAnimation(idleAnimation)
            }
        }

        private fun playScheduledAnimations(holder: PositionedHolder) {
            while (nextIndex < entries.size && entries[nextIndex].time <= elapsedTicks) {
                holder.animator.playAnimation(entries[nextIndex].animation)
                nextIndex++
            }
        }
    }
}
