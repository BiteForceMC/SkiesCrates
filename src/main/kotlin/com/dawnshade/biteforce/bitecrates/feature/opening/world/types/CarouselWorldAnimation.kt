package com.dawnshade.biteforce.bitecrates.feature.opening.world.types

import com.dawnshade.biteforce.bitecrates.config.block.HologramOptions
import com.dawnshade.biteforce.bitecrates.config.block.PreviewDisplayOptions
import com.dawnshade.biteforce.bitecrates.core.HologramsManager
import com.dawnshade.biteforce.bitecrates.feature.opening.world.WorldAnimationType
import com.dawnshade.biteforce.bitecrates.feature.opening.world.WorldOpeningAnimation
import com.dawnshade.biteforce.bitecrates.feature.opening.world.WorldOpeningInstance
import com.dawnshade.biteforce.bitecrates.feature.reward.Reward
import com.dawnshade.biteforce.bitecrates.integrations.ModIntegration
import com.dawnshade.biteforce.bitecrates.mixin.TextDisplayAccessor
import com.dawnshade.biteforce.bitecrates.util.TextUtils
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Brightness
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.EntityType
import net.minecraft.world.phys.Vec3
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class CarouselWorldAnimation(
    val radius: Double = 1.5,
    val height: Double = 1.0,
    val previewCount: Int = 8,
    val startDelay: Int = 0,
    val spinTicks: Int = 50,
    val slowTicks: Int = 30,
    val travelTicks: Int = 16,
    val orbitSpeed: Double = 0.35,
    val finalOrbitSpeed: Double = 0.04,
    val displaySpinSpeed: Float = 12.0f,
    val orbitUpdateInterval: Int = 3,
    val travelUpdateInterval: Int = 2,
    val offset: HologramOptions.XYZOption = HologramOptions.XYZOption(),
    val centerMode: CenterMode = CenterMode.CRATE,
    val hideHologram: Boolean = false,
) : WorldOpeningAnimation(WorldAnimationType.CAROUSEL) {
    companion object {
        const val PREVIEW_TAG = "bitecrates.carousel_preview"

        fun cleanupOrphanedDisplays(server: MinecraftServer) {
            for (level in server.allLevels) {
                val iterator = level.allEntities.iterator()
                while (iterator.hasNext()) {
                    val entity = iterator.next()
                    if (entity is Display.ItemDisplay && entity.tags.contains(PREVIEW_TAG)) {
                        entity.discard()
                    }
                }
            }
        }
    }

    @Transient private val displays = mutableListOf<PreviewDisplay>()
    @Transient private var elapsedTicks = 0
    @Transient private var orbitAngle = 0.0
    @Transient private var winningReward: Reward? = null
    @Transient private var winningDisplay: PreviewDisplay? = null
    @Transient private var phase: Phase = Phase.START
    @Transient private var travelStart: Vec3 = Vec3.ZERO
    @Transient private var lastOrbitSampleTick = 0
    @Transient private var lastTravelSampleTick = 0

    override fun setup(opening: WorldOpeningInstance) {
        clearDisplays()
        elapsedTicks = 0
        orbitAngle = 0.0
        phase = Phase.START
        travelStart = Vec3.ZERO
        lastOrbitSampleTick = 0
        lastTravelSampleTick = 0

        if (hideHologram && ModIntegration.HOLODISPLAYS.isModLoaded()) {
            HologramsManager.hideHologramForPlayer(opening.player, opening.instance)
        }

        winningReward = opening.randomBag.next()
        winningDisplay = null

        val previewRewards = buildPreviewRewards(opening)
        if (previewRewards.isEmpty()) {
            return
        }

        previewRewards.forEachIndexed { index, reward ->
            val angleOffset = (2.0 * PI * index) / previewRewards.size.toDouble()
            val spawnPos = getCenter(opening).add(
                cos(angleOffset) * radius,
                height,
                sin(angleOffset) * radius
            )
            val display = Display.ItemDisplay(EntityType.ITEM_DISPLAY, opening.instance.level)
            display.setNoGravity(true)
            display.isInvulnerable = true
            display.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, 0.0f, 0.0f)
            display.itemStack = reward.getDisplayItem(opening.player)
            display.itemTransform = ItemDisplayContext.GROUND
            display.billboardConstraints = Display.BillboardConstraints.FIXED
            display.brightnessOverride = Brightness.FULL_BRIGHT
            display.viewRange = 1.0f
            display.transformationInterpolationDuration = orbitUpdateInterval.coerceAtLeast(1)
            display.addTag(PREVIEW_TAG)
            opening.instance.level.addFreshEntity(display)
            val textDisplay = createTextDisplay(opening, reward, spawnPos)
            opening.instance.level.addFreshEntity(textDisplay)
            displays += PreviewDisplay(
                entity = display,
                textEntity = textDisplay,
                reward = reward,
                angleOffset = angleOffset
            )
        }

        winningDisplay = displays.firstOrNull { it.reward == winningReward } ?: displays.firstOrNull()
        sampleOrbit(opening, orbitSpeed, force = true)
    }

    override fun tick(opening: WorldOpeningInstance) {
        if (winningReward == null || displays.isEmpty()) {
            opening.chargeContext.refund()
            opening.stop()
            return
        }

        when (phase) {
            Phase.START -> {
                if (elapsedTicks++ >= startDelay) {
                    elapsedTicks = 0
                    lastOrbitSampleTick = 0
                    phase = Phase.SPIN
                }
                sampleOrbit(opening, orbitSpeed)
            }
            Phase.SPIN -> {
                elapsedTicks++
                sampleOrbit(opening, orbitSpeed)
                if (elapsedTicks >= spinTicks) {
                    elapsedTicks = 0
                    lastOrbitSampleTick = 0
                    phase = Phase.SLOW
                }
            }
            Phase.SLOW -> {
                elapsedTicks++
                val progress = (elapsedTicks.toDouble() / slowTicks.coerceAtLeast(1).toDouble()).coerceIn(0.0, 1.0)
                val currentSpeed = orbitSpeed + ((finalOrbitSpeed - orbitSpeed) * progress)
                sampleOrbit(opening, currentSpeed)
                if (elapsedTicks >= slowTicks) {
                    val winner = winningDisplay ?: displays.firstOrNull()
                    displays.filter { it !== winner }.forEach {
                        it.entity.discard()
                        it.textEntity.discard()
                    }
                    displays.removeIf { it !== winner }
                    winningDisplay = winner
                    travelStart = winner?.entity?.position() ?: getCenter(opening)
                    elapsedTicks = 0
                    lastTravelSampleTick = 0
                    winner?.entity?.transformationInterpolationDuration = travelUpdateInterval.coerceAtLeast(1)
                    winner?.textEntity?.transformationInterpolationDuration = travelUpdateInterval.coerceAtLeast(1)
                    phase = Phase.TRAVEL
                }
            }
            Phase.TRAVEL -> {
                elapsedTicks++
                val winner = winningDisplay ?: run {
                    opening.chargeContext.refund()
                    opening.stop()
                    return
                }
                sampleTravel(opening, winner)

                if (elapsedTicks >= travelTicks) {
                    completeOpening(opening)
                }
            }
        }
    }

    override fun stop(opening: WorldOpeningInstance) {
        clearDisplays()

        if (hideHologram && ModIntegration.HOLODISPLAYS.isModLoaded()) {
            HologramsManager.showHologramForPlayer(opening.player, opening.instance)
        }
    }

    private fun sampleOrbit(opening: WorldOpeningInstance, speed: Double, force: Boolean = false) {
        val interval = orbitUpdateInterval.coerceAtLeast(1)
        if (!force && elapsedTicks > 0 && elapsedTicks - lastOrbitSampleTick < interval) {
            return
        }

        val deltaTicks = if (force) interval else (elapsedTicks - lastOrbitSampleTick).coerceAtLeast(1)
        lastOrbitSampleTick = elapsedTicks
        orbitAngle += speed * deltaTicks

        val center = getCenter(opening)
        displays.forEachIndexed { index, preview ->
            val angle = orbitAngle + preview.angleOffset
            val bob = sin((orbitAngle * 0.5) + index) * 0.1
            val pos = center.add(
                cos(angle) * radius,
                height + bob,
                sin(angle) * radius
            )
            preview.entity.transformationInterpolationDuration = interval
            moveDisplay(preview.entity, pos, preview.spin)
            preview.textEntity.transformationInterpolationDuration = interval
            moveTextDisplay(opening, preview.textEntity, getLabelPosition(opening, pos))
            preview.spin += displaySpinSpeed * deltaTicks
        }
    }

    private fun sampleTravel(opening: WorldOpeningInstance, winner: PreviewDisplay, force: Boolean = false) {
        val interval = travelUpdateInterval.coerceAtLeast(1)
        if (!force && elapsedTicks > 0 && elapsedTicks - lastTravelSampleTick < interval) {
            return
        }

        val deltaTicks = if (force) interval else (elapsedTicks - lastTravelSampleTick).coerceAtLeast(1)
        lastTravelSampleTick = elapsedTicks
        val progress = (elapsedTicks.toDouble() / travelTicks.coerceAtLeast(1).toDouble()).coerceIn(0.0, 1.0)
        val target = opening.player.position().add(0.0, 1.0, 0.0)
        val pos = travelStart.lerp(target, progress)
        winner.entity.transformationInterpolationDuration = interval
        moveDisplay(winner.entity, pos, winner.spin)
        winner.textEntity.transformationInterpolationDuration = interval
        moveTextDisplay(opening, winner.textEntity, getLabelPosition(opening, pos))
        winner.spin += displaySpinSpeed * deltaTicks
    }

    private fun getCenter(opening: WorldOpeningInstance): Vec3 {
        val base = when (centerMode) {
            CenterMode.PLAYER -> opening.player.position()
            CenterMode.CRATE -> opening.instance.pos.bottomCenter
        }
        return base.add(offset.x.toDouble(), offset.y.toDouble(), offset.z.toDouble())
    }

    private fun moveDisplay(display: Display.ItemDisplay, pos: Vec3, spin: Float) {
        display.moveTo(pos.x, pos.y, pos.z, spin, 0.0f)
    }

    private fun moveTextDisplay(opening: WorldOpeningInstance, display: Display.TextDisplay, pos: Vec3) {
        display.moveTo(pos.x, pos.y, pos.z, resolveLabelYaw(opening, pos), 0.0f)
    }

    private fun completeOpening(opening: WorldOpeningInstance) {
        val reward = winningReward ?: run {
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
        opening.stop()
    }

    private fun buildPreviewRewards(opening: WorldOpeningInstance): List<Reward> {
        val winningReward = winningReward ?: return emptyList()
        val previewBag = opening.crate.generateRewardBag(opening.chargeContext.userData)
        val rewards = mutableListOf<Reward>()
        val count = previewCount.coerceAtLeast(1)

        rewards += winningReward
        while (rewards.size < count) {
            rewards += previewBag.next() ?: winningReward
        }

        return rewards.shuffled()
    }

    private fun clearDisplays() {
        displays.forEach {
            it.entity.discard()
            it.textEntity.discard()
        }
        displays.clear()
    }

    private fun createTextDisplay(opening: WorldOpeningInstance, reward: Reward, itemPos: Vec3): Display.TextDisplay {
        val textDisplay = Display.TextDisplay(EntityType.TEXT_DISPLAY, opening.instance.level)
        val labelPos = getLabelPosition(opening, itemPos)
        textDisplay.moveTo(labelPos.x, labelPos.y, labelPos.z, resolveLabelYaw(opening, labelPos), 0.0f)
        textDisplay.billboardConstraints = Display.BillboardConstraints.FIXED
        textDisplay.brightnessOverride = Brightness.FULL_BRIGHT
        textDisplay.viewRange = 1.0f
        textDisplay.transformationInterpolationDuration = orbitUpdateInterval.coerceAtLeast(1)
        textDisplay.entityData.set(TextDisplayAccessor.getText(), TextUtils.toNative(reward.name))
        textDisplay.entityData.set(TextDisplayAccessor.getLineWidth(), 200)
        textDisplay.entityData.set(TextDisplayAccessor.getBackgroundColor(), 0)
        textDisplay.entityData.set(TextDisplayAccessor.getTextOpacity(), (-1).toByte())
        textDisplay.entityData.set(TextDisplayAccessor.getStyleFlags(), TextDisplayAccessor.getFlagShadow())
        textDisplay.addTag(PREVIEW_TAG)
        return textDisplay
    }

    private fun getLabelPosition(opening: WorldOpeningInstance, itemPos: Vec3): Vec3 {
        val labelOffset = opening.instance.previewDisplay?.labelOffset ?: HologramOptions.XYZOption(0.0f, 0.75f, 0.0f)
        return itemPos.add(labelOffset.x.toDouble(), labelOffset.y.toDouble(), labelOffset.z.toDouble())
    }

    private fun resolveLabelYaw(opening: WorldOpeningInstance, textPos: Vec3): Float {
        val previewDisplay = opening.instance.previewDisplay
        if (previewDisplay?.labelFacePlayer != true) {
            return previewDisplay?.labelYawOffset ?: 0.0f
        }

        val delta = opening.initialPlayerPosition.subtract(textPos)
        val yaw = Math.toDegrees(atan2(delta.z, delta.x)).toFloat() - 90.0f
        return yaw + previewDisplay.labelYawOffset
    }

    private data class PreviewDisplay(
        val entity: Display.ItemDisplay,
        val textEntity: Display.TextDisplay,
        val reward: Reward,
        val angleOffset: Double,
        var spin: Float = 0.0f,
    )

    enum class CenterMode {
        CRATE,
        PLAYER
    }

    private enum class Phase {
        START,
        SPIN,
        SLOW,
        TRAVEL
    }
}
