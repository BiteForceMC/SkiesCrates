package com.pokeskies.skiescrates.data.opening.world.types

import com.google.gson.annotations.SerializedName
import com.pokeskies.skiescrates.config.SoundOption
import com.pokeskies.skiescrates.data.opening.world.RewardItemEntity
import com.pokeskies.skiescrates.data.opening.world.WorldAnimationType
import com.pokeskies.skiescrates.data.opening.world.WorldOpeningAnimation
import com.pokeskies.skiescrates.data.opening.world.WorldOpeningInstance
import com.pokeskies.skiescrates.data.rewards.Reward
import com.pokeskies.skiescrates.integrations.ModIntegration
import com.pokeskies.skiescrates.managers.HologramsManager
import com.pokeskies.skiescrates.mixins.EntityAccessor
import com.pokeskies.skiescrates.mixins.ItemEntityAccessor
import com.pokeskies.skiescrates.utils.TextUtils
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.entity.EntityType
import net.minecraft.world.phys.Vec3
import java.util.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class OrbitalEliminationWorldAnimation(
    @SerializedName("item_count")
    val itemCount: Int = 8,
    @SerializedName("start_delay")
    val startDelay: Int = 10,
    @SerializedName("pop_out_ticks")
    val popOutTicks: Int = 20,
    @SerializedName("spin_ticks")
    val spinTicks: Int = 80,
    @SerializedName("elimination_interval")
    val eliminationInterval: Int = 10,
    @SerializedName("final_hold_ticks")
    val finalHoldTicks: Int = 40,
    @SerializedName("orbit_radius")
    val orbitRadius: Double = 1.2,
    @SerializedName("orbit_height")
    val orbitHeight: Double = 1.0,
    @SerializedName("base_angular_speed")
    val baseAngularSpeed: Double = 0.12,
    @SerializedName("angular_acceleration")
    val angularAcceleration: Double = 0.006,
    @SerializedName("max_angular_speed")
    val maxAngularSpeed: Double = 0.7,
    @SerializedName("bob_height")
    val bobHeight: Double = 0.08,
    val offset: Vec3 = Vec3.ZERO,
    @SerializedName(value = "hide_hologram")
    val hideHologram: Boolean = false,
    @SerializedName("play_model_animation")
    val playModelAnimation: Boolean = true,
    @SerializedName("open_model_animation")
    val openModelAnimation: String? = null,
    @SerializedName("close_model_animation")
    val closeModelAnimation: String? = null,
    @SerializedName("open_sound")
    val openSound: SoundOption? = SoundOption("minecraft:block.chest.open", 0.8f, 1.0f, "BLOCKS"),
    @SerializedName("pop_sound")
    val popSound: SoundOption? = SoundOption("minecraft:entity.item.break", 0.9f, 1.2f, "PLAYERS"),
    @SerializedName("win_sound")
    val winSound: SoundOption? = SoundOption("minecraft:entity.player.levelup", 1.0f, 1.1f, "PLAYERS"),
): WorldOpeningAnimation(WorldAnimationType.ORBITAL_ELIMINATION) {
    private enum class Phase {
        START_DELAY,
        POP_OUT,
        SPIN,
        ELIMINATE,
        FINAL_SHOW
    }

    private data class OrbitingEntry(
        val reward: Reward,
        val entity: RewardItemEntity,
        val winner: Boolean,
        var angle: Double,
        var alive: Boolean = true
    )

    @Transient private val orbitEntries: MutableList<OrbitingEntry> = mutableListOf()
    @Transient private var winnerEntry: OrbitingEntry? = null
    @Transient private var phase: Phase = Phase.START_DELAY
    @Transient private var phaseTicks = 0
    @Transient private var eliminationTicks = 0
    @Transient private var angularSpeed = 0.0
    @Transient private var center = Vec3.ZERO
    @Transient private var rewardGiven = false

    override fun setup(opening: WorldOpeningInstance) {
        orbitEntries.clear()
        winnerEntry = null

        phase = Phase.START_DELAY
        phaseTicks = 0
        eliminationTicks = 0
        angularSpeed = baseAngularSpeed
        rewardGiven = false

        center = opening.instance.pos.bottomCenter.add(offset).add(0.0, orbitHeight, 0.0)

        val winnerReward = generateReward(opening) ?: run {
            opening.stop()
            return
        }

        val totalEntries = itemCount.coerceAtLeast(1)
        val winnerSlot = Random.nextInt(totalEntries)
        for (index in 0 until totalEntries) {
            val reward = if (index == winnerSlot) {
                winnerReward
            } else {
                generateReward(opening) ?: winnerReward
            }

            val entry = OrbitingEntry(
                reward = reward,
                entity = RewardItemEntity(opening.instance.level, center, reward.getDisplayItem(opening.player)),
                winner = index == winnerSlot,
                angle = (index.toDouble() / totalEntries.toDouble()) * (2.0 * PI),
            )
            orbitEntries += entry
            if (entry.winner) {
                winnerEntry = entry
            }

            spawnEntity(opening, entry)
        }

        if (hideHologram && ModIntegration.HOLODISPLAYS.isModLoaded()) {
            HologramsManager.hideHologramForPlayer(opening.player, opening.instance)
        }

        playModelAnimation(opening, openModelAnimation ?: opening.instance.model?.animations?.open)
        openSound?.playSound(opening.player)
    }

    override fun tick(opening: WorldOpeningInstance) {
        if (orbitEntries.isEmpty()) {
            opening.stop()
            return
        }

        when (phase) {
            Phase.START_DELAY -> {
                phaseTicks++
                if (phaseTicks >= startDelay.coerceAtLeast(0)) {
                    phase = Phase.POP_OUT
                    phaseTicks = 0
                }
            }
            Phase.POP_OUT -> {
                phaseTicks++
                tickOrbiting(opening, getPopOutRadius())
                if (phaseTicks >= popOutTicks.coerceAtLeast(1)) {
                    phase = Phase.SPIN
                    phaseTicks = 0
                }
            }
            Phase.SPIN -> {
                phaseTicks++
                tickOrbiting(opening, orbitRadius)
                if (phaseTicks >= spinTicks.coerceAtLeast(0)) {
                    phase = Phase.ELIMINATE
                    phaseTicks = 0
                    eliminationTicks = 0
                }
            }
            Phase.ELIMINATE -> {
                phaseTicks++
                eliminationTicks++
                tickOrbiting(opening, orbitRadius)

                if (eliminationTicks >= eliminationInterval.coerceAtLeast(1)) {
                    eliminationTicks = 0
                    eliminateOne(opening)
                }

                if (orbitEntries.count { it.alive } <= 1) {
                    phase = Phase.FINAL_SHOW
                    phaseTicks = 0
                    onFinalReveal(opening)
                }
            }
            Phase.FINAL_SHOW -> {
                phaseTicks++
                tickFinalReveal(opening)
                if (phaseTicks >= finalHoldTicks.coerceAtLeast(1)) {
                    opening.stop()
                }
            }
        }
    }

    override fun stop(opening: WorldOpeningInstance) {
        removeAliveEntities(opening)
        orbitEntries.clear()

        if (hideHologram && ModIntegration.HOLODISPLAYS.isModLoaded()) {
            HologramsManager.showHologramForPlayer(opening.player, opening.instance)
        }

        playModelAnimation(opening, closeModelAnimation ?: opening.instance.model?.animations?.close)
    }

    private fun tickOrbiting(opening: WorldOpeningInstance, radius: Double) {
        angularSpeed = (angularSpeed + angularAcceleration).coerceAtMost(maxAngularSpeed)

        orbitEntries.filter { it.alive }.forEach { entry ->
            entry.angle += angularSpeed

            val x = center.x + cos(entry.angle) * radius
            val y = center.y + sin((phaseTicks * 0.22) + entry.angle) * bobHeight
            val z = center.z + sin(entry.angle) * radius

            entry.entity.setPos(x, y, z)
            opening.player.connection.send(ClientboundTeleportEntityPacket(entry.entity))
        }
    }

    private fun getPopOutRadius(): Double {
        val target = orbitRadius.coerceAtLeast(0.0)
        val duration = popOutTicks.coerceAtLeast(1).toDouble()
        val progress = (phaseTicks.toDouble() / duration).coerceIn(0.0, 1.0)
        return target * progress
    }

    private fun eliminateOne(opening: WorldOpeningInstance) {
        val removable = orbitEntries.filter { it.alive && !it.winner }
        if (removable.isEmpty()) return

        val removed = removable.random()
        removed.alive = false
        opening.player.connection.send(ClientboundRemoveEntitiesPacket(removed.entity.id))
        popSound?.playSound(opening.player)
    }

    private fun onFinalReveal(opening: WorldOpeningInstance) {
        val winner = winnerEntry ?: return
        if (!winner.alive) {
            winner.alive = true
        }

        if (!rewardGiven) {
            rewardGiven = true
            winner.reward.giveReward(opening.player, opening.crate)
            winSound?.playSound(opening.player)
        }

        opening.player.connection.send(
            ClientboundSetEntityDataPacket(
                winner.entity.id,
                listOf(
                    SynchedEntityData.DataValue.create(EntityAccessor.getCustomName(), Optional.of(TextUtils.toNative(winner.reward.name))),
                    SynchedEntityData.DataValue.create(EntityAccessor.getCustomNameVisible(), true)
                )
            )
        )
    }

    private fun tickFinalReveal(opening: WorldOpeningInstance) {
        val winner = winnerEntry ?: return
        if (!winner.alive) return

        winner.angle += 0.35
        val y = center.y + sin(phaseTicks * 0.25) * (bobHeight * 1.5)
        winner.entity.setPos(center.x, y, center.z)
        opening.player.connection.send(ClientboundTeleportEntityPacket(winner.entity))
    }

    private fun spawnEntity(opening: WorldOpeningInstance, entry: OrbitingEntry) {
        val entity = entry.entity
        opening.player.connection.send(
            ClientboundAddEntityPacket(
                entity.id,
                entity.uuid,
                center.x,
                center.y,
                center.z,
                0f,
                0f,
                EntityType.ITEM,
                0,
                Vec3.ZERO,
                0.0
            )
        )

        opening.player.connection.send(
            ClientboundSetEntityDataPacket(
                entity.id,
                listOf(
                    SynchedEntityData.DataValue.create(EntityAccessor.getNoGravity(), true),
                    SynchedEntityData.DataValue.create(EntityAccessor.getCustomName(), Optional.of(TextUtils.toNative(entry.reward.name))),
                    SynchedEntityData.DataValue.create(EntityAccessor.getCustomNameVisible(), false),
                    SynchedEntityData.DataValue.create(ItemEntityAccessor.getItem(), entity.item),
                )
            )
        )
    }

    private fun removeAliveEntities(opening: WorldOpeningInstance) {
        val ids = orbitEntries.filter { it.alive }.map { it.entity.id }.toIntArray()
        if (ids.isNotEmpty()) {
            opening.player.connection.send(ClientboundRemoveEntitiesPacket(*ids))
        }
    }

    private fun playModelAnimation(opening: WorldOpeningInstance, animation: String?) {
        if (!playModelAnimation || animation.isNullOrEmpty()) return

        try {
            opening.instance.bilData?.holder?.animator?.playAnimation(animation)
        } catch (_: Exception) {
            // Not all models contain each animation name; fail silently.
        }
    }

    private fun generateReward(opening: WorldOpeningInstance): Reward? {
        if (opening.randomBag.size() <= 0) return null
        return opening.randomBag.next()
    }
}
