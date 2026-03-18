package com.pokeskies.skiescrates.integrations.bil

import com.pokeskies.skiescrates.SkiesCrates.Companion.asyncScope
import com.pokeskies.skiescrates.config.block.ModelOptions
import com.pokeskies.skiescrates.data.CrateInstance
import com.pokeskies.skiescrates.data.CrateOpenData
import com.pokeskies.skiescrates.integrations.ModIntegration
import com.pokeskies.skiescrates.managers.CratesManager.openAllCratesWithKeys
import com.pokeskies.skiescrates.managers.CratesManager.openCrate
import com.pokeskies.skiescrates.managers.CratesManager.previewCrate
import com.pokeskies.skiescrates.utils.Utils
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
    var holder: PositionedHolder
) {
    fun isAttached(): Boolean {
        return holder.attachment != null
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
                modelOptions.offset
            )
            holder.scale = modelOptions.scale
            modelOptions.animations.idle?.let { holder.animator.playAnimation(it) }
            val element = InteractionElement()
            element.setSize(modelOptions.hitbox.width, modelOptions.hitbox.height)
            element.offset = modelOptions.hitbox.offset
            element.setHandler(object : InteractionHandler {
                override fun interactAt(player: ServerPlayer, hand: InteractionHand, pos: Vec3) {
                    asyncScope.launch {
                        if (player.isShiftKeyDown) {
                            openAllCratesWithKeys(player, instance.crate, CrateOpenData(instance.dimPos, null))
                        } else {
                            openCrate(player, instance.crate, CrateOpenData(instance.dimPos, null), false)
                        }
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

            return BILCrateData(holder)
        }
    }
}
