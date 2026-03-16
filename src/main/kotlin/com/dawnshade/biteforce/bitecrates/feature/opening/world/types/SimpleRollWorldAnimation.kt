package com.dawnshade.biteforce.bitecrates.feature.opening.world.types

import com.google.gson.annotations.SerializedName
import com.dawnshade.biteforce.bitecrates.config.block.HologramOptions
import com.dawnshade.biteforce.bitecrates.config.block.PreviewDisplayOptions
import com.dawnshade.biteforce.bitecrates.config.SoundOption
import com.dawnshade.biteforce.bitecrates.feature.opening.world.RewardItemEntity
import com.dawnshade.biteforce.bitecrates.feature.opening.world.WorldAnimationType
import com.dawnshade.biteforce.bitecrates.feature.opening.world.WorldOpeningAnimation
import com.dawnshade.biteforce.bitecrates.feature.opening.world.WorldOpeningInstance
import com.dawnshade.biteforce.bitecrates.feature.reward.Reward
import com.dawnshade.biteforce.bitecrates.integrations.ModIntegration
import com.dawnshade.biteforce.bitecrates.core.HologramsManager
import com.dawnshade.biteforce.bitecrates.mixin.EntityAccessor
import com.dawnshade.biteforce.bitecrates.mixin.ItemEntityAccessor
import com.dawnshade.biteforce.bitecrates.mixin.TextDisplayAccessor
import com.dawnshade.biteforce.bitecrates.util.TextUtils
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.EntityType
import net.minecraft.world.phys.Vec3
import java.util.Optional
import kotlin.math.atan2


class SimpleRollWorldAnimation(
    @SerializedName("spin_count")
    val spinCount: Int = 10,
    @SerializedName("spin_interval")
    val spinInterval: Int = 1,
    @SerializedName("start_delay")
    val startDelay: Int = 5,
    @SerializedName("change_interval")
    val changeInterval: Int = 5,
    @SerializedName("change_amount")
    val changeAmount: Int = 1,
    @SerializedName("end_delay")
    val endDelay: Int = 20,
    val sound: SoundOption? = null,
    val offset: HologramOptions.XYZOption = HologramOptions.XYZOption(),
    @SerializedName(value = "hide_hologram")
    val hideHologram: Boolean = false,
): WorldOpeningAnimation(WorldAnimationType.SIMPLE_ROLL) {
    @Transient private lateinit var pregeneratedSlots: MutableList<Reward>
    @Transient private var currentIndex = 0
    @Transient private var currentReward: Reward? = null
    @Transient private var itemEntity: RewardItemEntity? = null
    @Transient private var textDisplay: Display.TextDisplay? = null

    @Transient private var isStarted = false
    @Transient private var isCompleted = false
    @Transient private var ticks = startDelay

    @Transient private var spinsRemaining = spinCount
    @Transient private var ticksPerSpin = spinInterval
    @Transient private var ticksUntilChange = changeInterval

    @Transient private var pos = Vec3.ZERO

    override fun setup(opening: WorldOpeningInstance) {
        pregeneratedSlots = List(spinCount) { generateItem(opening) }.filterNotNull().toMutableList()

        currentIndex = 0
        currentReward = null
        itemEntity = null
        textDisplay = null

        isStarted = false
        isCompleted = false
        ticks = startDelay

        spinsRemaining = spinCount
        ticksPerSpin = spinInterval
        ticksUntilChange = changeInterval

        pos = opening.instance.pos.bottomCenter.add(
            offset.x.toDouble(),
            offset.y.toDouble(),
            offset.z.toDouble()
        )

        if (hideHologram && ModIntegration.HOLODISPLAYS.isModLoaded()) {
            HologramsManager.hideHologramForPlayer(opening.player, opening.instance)
        }
    }

    override fun tick(opening: WorldOpeningInstance) {
        if (isStarted && !isCompleted) {
            ticks--
            if (ticks <= 0) {
                ticksUntilChange--
                if (ticksUntilChange <= 0) {
                    ticksUntilChange = changeInterval
                    ticksPerSpin += changeAmount
                }
                ticks = ticksPerSpin
                spinsRemaining--

                spin(opening)

                if (spinsRemaining <= 0) {
                    completeOpening(opening)
                }
            }
        } else if (isCompleted) {
            ticks--
            if (ticks <= 0) {
                opening.stop()
            }
        } else {
            ticks--
            if (ticks <= 0) {
                isStarted = true
                ticks = ticksPerSpin

                spin(opening)

                if (--spinsRemaining <= 0) {
                    completeOpening(opening)
                }
            }
        }
    }

    override fun stop(opening: WorldOpeningInstance) {
        val entityIds = mutableListOf<Int>()
        itemEntity?.let { entityIds += it.id }
        textDisplay?.let { entityIds += it.id }
        if (entityIds.isNotEmpty()) {
            opening.player.connection.send(ClientboundRemoveEntitiesPacket(*entityIds.toIntArray()))
        }

        if (hideHologram && ModIntegration.HOLODISPLAYS.isModLoaded()) {
            HologramsManager.showHologramForPlayer(opening.player, opening.instance)
        }
    }

    private fun spin(opening: WorldOpeningInstance) {
        val newReward = pregeneratedSlots.getOrNull(currentIndex++)
        currentReward = newReward

        if (newReward != null) {
            if (itemEntity == null) {
                itemEntity = RewardItemEntity(
                    opening.instance.level,
                    pos,
                    newReward.getDisplayItem(opening.player)
                )
                textDisplay = createTextDisplay(opening, newReward)
                opening.player.connection.send(
                    ClientboundAddEntityPacket(
                        itemEntity!!.id,
                        itemEntity!!.uuid,
                        pos.x,
                        pos.y,
                        pos.z,
                        0f,
                        0f,
                        EntityType.ITEM,
                        0,
                        Vec3.ZERO,
                        0.0
                    )
                )
                opening.player.connection.send(
                    ClientboundAddEntityPacket(
                        textDisplay!!.id,
                        textDisplay!!.uuid,
                        textDisplay!!.x,
                        textDisplay!!.y,
                        textDisplay!!.z,
                        0f,
                        textDisplay!!.yRot,
                        EntityType.TEXT_DISPLAY,
                        0,
                        Vec3.ZERO,
                        textDisplay!!.yRot.toDouble()
                    )
                )
                opening.player.connection.send(ClientboundSetEntityDataPacket(itemEntity!!.id, listOf(
                    SynchedEntityData.DataValue.create(EntityAccessor.getNoGravity(), true),
                    SynchedEntityData.DataValue.create(EntityAccessor.getCustomNameVisible(), false),
                    SynchedEntityData.DataValue.create(ItemEntityAccessor.getItem(), itemEntity!!.item)
                )))
                opening.player.connection.send(ClientboundSetEntityDataPacket(textDisplay!!.id, listOf(
                    SynchedEntityData.DataValue.create(TextDisplayAccessor.getText(), TextUtils.toNative(newReward.name)),
                    SynchedEntityData.DataValue.create(TextDisplayAccessor.getLineWidth(), 200),
                    SynchedEntityData.DataValue.create(TextDisplayAccessor.getBackgroundColor(), 0),
                    SynchedEntityData.DataValue.create(TextDisplayAccessor.getTextOpacity(), (-1).toByte()),
                    SynchedEntityData.DataValue.create(TextDisplayAccessor.getStyleFlags(), TextDisplayAccessor.getFlagShadow())
                )))
            } else {
                itemEntity!!.item = newReward.getDisplayItem(opening.player)
                updateTextDisplayPosition(opening, textDisplay!!)
                opening.player.connection.send(ClientboundSetEntityDataPacket(itemEntity!!.id, listOf(
                    SynchedEntityData.DataValue.create(ItemEntityAccessor.getItem(), itemEntity!!.item)
                )))
                opening.player.connection.send(ClientboundSetEntityDataPacket(textDisplay!!.id, listOf(
                    SynchedEntityData.DataValue.create(TextDisplayAccessor.getText(), TextUtils.toNative(newReward.name))
                )))
            }
        }

        sound?.playSound(opening.player)
    }

    private fun generateItem(opening: WorldOpeningInstance): Reward? {
        if (opening.randomBag.size() <= 0) return null
        return opening.randomBag.next()
    }

    private fun completeOpening(opening: WorldOpeningInstance) {
        isCompleted = true
        ticks = endDelay
        val reward = currentReward ?: run {
            opening.chargeContext.refund()
            opening.stop()
            return
        }
        if (!reward.giveReward(opening.player, opening.crate)) {
            opening.chargeContext.refund()
            opening.stop()
            return
        }
        opening.chargeContext.complete(listOf(reward))
    }

    private fun createTextDisplay(opening: WorldOpeningInstance, reward: Reward): Display.TextDisplay {
        val textDisplay = Display.TextDisplay(EntityType.TEXT_DISPLAY, opening.instance.level)
        updateTextDisplayPosition(opening, textDisplay)
        textDisplay.entityData.set(TextDisplayAccessor.getText(), TextUtils.toNative(reward.name))
        textDisplay.entityData.set(TextDisplayAccessor.getLineWidth(), 200)
        textDisplay.entityData.set(TextDisplayAccessor.getBackgroundColor(), 0)
        textDisplay.entityData.set(TextDisplayAccessor.getTextOpacity(), (-1).toByte())
        textDisplay.entityData.set(TextDisplayAccessor.getStyleFlags(), TextDisplayAccessor.getFlagShadow())
        return textDisplay
    }

    private fun updateTextDisplayPosition(opening: WorldOpeningInstance, textDisplay: Display.TextDisplay) {
        val previewDisplay = opening.instance.previewDisplay
        val labelOffset = previewDisplay?.labelOffset ?: HologramOptions.XYZOption(0.0f, 0.75f, 0.0f)
        val textPos = pos.add(labelOffset.x.toDouble(), labelOffset.y.toDouble(), labelOffset.z.toDouble())
        val yaw = resolveLabelYaw(opening, previewDisplay, textPos)
        textDisplay.moveTo(textPos.x, textPos.y, textPos.z, yaw, 0.0f)
    }

    private fun resolveLabelYaw(opening: WorldOpeningInstance, previewDisplay: PreviewDisplayOptions?, textPos: Vec3): Float {
        if (previewDisplay?.labelFacePlayer != true) {
            return previewDisplay?.labelYawOffset ?: 0.0f
        }

        val delta = opening.initialPlayerPosition.subtract(textPos)
        val yaw = Math.toDegrees(atan2(delta.z, delta.x)).toFloat() - 90.0f
        return yaw + previewDisplay.labelYawOffset
    }
}
