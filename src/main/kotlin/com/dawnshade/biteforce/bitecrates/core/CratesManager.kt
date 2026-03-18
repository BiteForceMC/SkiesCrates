package com.dawnshade.biteforce.bitecrates.core

import com.dawnshade.biteforce.bitecrates.core.BiteCrates
import com.dawnshade.biteforce.bitecrates.core.BiteCrates.Companion.asyncScope
import com.dawnshade.biteforce.bitecrates.config.ConfigManager
import com.dawnshade.biteforce.bitecrates.config.Lang
import com.dawnshade.biteforce.bitecrates.config.block.CrateBlockLocation
import com.dawnshade.biteforce.bitecrates.data.Crate
import com.dawnshade.biteforce.bitecrates.data.CrateInstance
import com.dawnshade.biteforce.bitecrates.data.CrateOpenData
import com.dawnshade.biteforce.bitecrates.data.DimensionalBlockPos
import com.dawnshade.biteforce.bitecrates.feature.opening.OpenChargeContext
import com.dawnshade.biteforce.bitecrates.feature.opening.inventory.InventoryOpeningAnimation
import com.dawnshade.biteforce.bitecrates.feature.opening.inventory.InventoryOpeningInstance
import com.dawnshade.biteforce.bitecrates.feature.opening.world.WorldOpeningAnimation
import com.dawnshade.biteforce.bitecrates.feature.opening.world.WorldOpeningInstance
import com.dawnshade.biteforce.bitecrates.economy.EconomyManager
import com.dawnshade.biteforce.bitecrates.events.ItemSwingEvent
import com.dawnshade.biteforce.bitecrates.gui.PreviewInventory
import com.dawnshade.biteforce.bitecrates.integrations.bil.BILCrateData
import com.dawnshade.biteforce.bitecrates.util.ChunkKey
import com.dawnshade.biteforce.bitecrates.util.MinecraftDispatcher
import com.dawnshade.biteforce.bitecrates.util.TextUtils
import com.dawnshade.biteforce.bitecrates.util.Utils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.phys.Vec3
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

object CratesManager {
    const val CRATE_IDENTIFIER: String = "${BiteCrates.MOD_ID}:crate"
    private val instancesByChunk: MutableMap<ChunkKey, MutableMap<DimensionalBlockPos, CrateInstance>> = mutableMapOf()
    private val interactionLimiter = mutableMapOf<UUID, Long>()
    private val playerOpenLocks = ConcurrentHashMap<UUID, Mutex>()

    fun init() {
        instancesByChunk.values.forEach { chunkInstances ->
            chunkInstances.values.forEach { it.destroy() }
        }
        instancesByChunk.clear()

        ConfigManager.CRATES.forEach { (_, crate) ->
            loadCrate(crate)
        }

        registerEvents()
    }

    fun registerEvents() {
        ServerPlayConnectionEvents.JOIN.register(ServerPlayConnectionEvents.Join { handler, _, _ ->
            KeyManager.preloadKeys(handler.player)
            OpeningManager.getInstance(handler.player.uuid)?.stop()
        })

        PlayerBlockBreakEvents.BEFORE.register(PlayerBlockBreakEvents.Before { level, _, blockPos, _, _ ->
            val dimensionalPos = DimensionalBlockPos(
                level.dimension().location().toString(),
                blockPos.x,
                blockPos.y,
                blockPos.z
            )
            getCrateFromPos(dimensionalPos)?.let { _ ->
                return@Before false
            }
            return@Before true
        })
        AttackBlockCallback.EVENT.register(AttackBlockCallback { player, level, _, blockPos, _ ->
            if (player !is ServerPlayer) return@AttackBlockCallback InteractionResult.PASS

            val dimensionalPos = DimensionalBlockPos(
                level.dimension().location().toString(),
                blockPos.x,
                blockPos.y,
                blockPos.z
            )
            getCrateFromPos(dimensionalPos)?.let { instance ->
                previewCrate(player, instance.crate)
                return@AttackBlockCallback InteractionResult.FAIL
            }
            return@AttackBlockCallback InteractionResult.PASS
        })
        UseBlockCallback.EVENT.register(UseBlockCallback { player, level, hand, blockHitResult ->
            if (player !is ServerPlayer) return@UseBlockCallback InteractionResult.PASS
            if (hand != InteractionHand.MAIN_HAND) return@UseBlockCallback InteractionResult.PASS

            val blockPos = DimensionalBlockPos(
                level.dimension().location().toString(),
                blockHitResult.blockPos.x,
                blockHitResult.blockPos.y,
                blockHitResult.blockPos.z
            )
            getCrateFromPos(blockPos)?.let { instance ->
                asyncScope.launch {
                    if (player.isShiftKeyDown) {
                        openAllCratesWithKeys(player, instance.crate, CrateOpenData(blockPos, null))
                    } else {
                        openCrate(player, instance.crate, CrateOpenData(blockPos, null), false)
                    }
                }
                return@UseBlockCallback InteractionResult.FAIL
            }

            val item = player.getItemInHand(hand)
            if (!item.isEmpty) {
                val crate = getCrateOrNull(item)
                if (crate != null) {
                    asyncScope.launch {
                        openCrate(player, crate, CrateOpenData(null, item), false)
                    }
                    return@UseBlockCallback InteractionResult.FAIL
                }

                val keyId = KeyManager.getKeyOrNull(item)
                if (keyId != null) {
                    return@UseBlockCallback InteractionResult.FAIL
                }
            }

            return@UseBlockCallback InteractionResult.PASS
        })
        UseItemCallback.EVENT.register(UseItemCallback { player, _, hand ->
            if (player !is ServerPlayer) return@UseItemCallback InteractionResultHolder.pass(player.getItemInHand(hand))

            val item = player.getItemInHand(hand)
            if (hand != InteractionHand.MAIN_HAND) return@UseItemCallback InteractionResultHolder.pass(item)

            getCrateOrNull(item)?.let { crate ->
                asyncScope.launch {
                    openCrate(player, crate, CrateOpenData(null, item), false)
                }
                return@UseItemCallback InteractionResultHolder.fail(item)
            }

            KeyManager.getKeyOrNull(item)?.let { _ ->
                return@UseItemCallback InteractionResultHolder.fail(item)
            }

            return@UseItemCallback InteractionResultHolder.pass(item)
        })
        ItemSwingEvent.EVENT.register { player, itemStack, _ ->
            BiteCrates.INSTANCE.server.execute {
                val crate = getCrateOrNull(itemStack) ?: return@execute
                previewCrate(player, crate)
            }

            return@register InteractionResult.PASS
        }

        ServerChunkEvents.CHUNK_LOAD.register { level, chunk ->
            val unattached = getInstancesAtChunk(level, chunk).filter {
                it.bilData?.let { bilData -> !bilData.isAttached() } ?: false
            }

            for (instance in unattached) {
                instance.model?.let { modelOptions ->
                    instance.bilData = BILCrateData.create(instance, chunk, modelOptions)
                }
            }
        }
    }

    fun tick() {
        instancesByChunk.forEach { (_, chunkInstances) -> chunkInstances.values.forEach { it.tick() } }
    }

    fun loadCrate(crate: Crate) {
        if (!crate.enabled) return
        for (blockLocation in crate.block.locations) {
            loadCrateLocation(crate, blockLocation)
        }
    }

    fun loadCrateLocation(crate: Crate, blockLocation: CrateBlockLocation): CrateInstance? {
        val location = blockLocation.getDimensionalBlockPos()

        val level = Utils.getLevel(location.dimension)
        if (level == null) {
            Utils.printError("Crate ${crate.name} has an invalid dimension location: $location")
            return null
        }

        val key = ChunkKey.of(location)
        val chunkInstances = getChunkInstances(key)
        if (chunkInstances.containsKey(location)) {
            Utils.printError("Crate ${crate.name} has a duplicate location: $location")
            return null
        }

        val instance = CrateInstance(
            crate,
            level,
            location.getBlockPos(),
            location,
            blockLocation.model ?: crate.block.model,
            blockLocation.hologram ?: crate.block.hologram,
            blockLocation.previewDisplay ?: crate.block.previewDisplay,
            ConfigManager.PARTICLES[blockLocation.particles ?: crate.block.particle]
        )
        chunkInstances[location] = instance
        return instance
    }

    fun unloadCrateLocation(instance: CrateInstance): Boolean {
        instance.destroy()
        val key = ChunkKey.of(instance.dimPos)
        val chunkInstances = instancesByChunk[key] ?: return false
        val removed = chunkInstances.remove(instance.dimPos) != null
        if (chunkInstances.isEmpty()) instancesByChunk.remove(key)
        return removed
    }

    fun giveCrates(crate: Crate, player: ServerPlayer, amount: Int, silent: Boolean = false): Boolean {
        val item = crate.display.createItemStack(player)

        item.count = amount

        val tag = CompoundTag()
        tag.putString(CRATE_IDENTIFIER, crate.id)
        item.applyComponents(
            DataComponentPatch.builder()
            .set(DataComponents.CUSTOM_DATA, CustomData.of(tag))
            .build())

        player.inventory.placeItemBackInInventory(item)

        if (!silent) {
            Lang.CRATE_GIVE.forEach {
                player.sendMessage(TextUtils.parseAllNative(player, crate.parsePlaceholders(it)))
            }
        }

        return true
    }

    suspend fun openCrate(
        player: ServerPlayer,
        crate: Crate,
        openData: CrateOpenData,
        isForced: Boolean,
        ignoreInteractionLimiter: Boolean = false,
        ignoreCooldown: Boolean = false,
        forceInstantOpen: Boolean = false
    ): Boolean {
        val playerLock = playerOpenLocks.computeIfAbsent(player.uuid) { Mutex() }
        return playerLock.withLock {
            openCrateLocked(
                player,
                crate,
                openData,
                isForced,
                ignoreInteractionLimiter,
                ignoreCooldown,
                forceInstantOpen
            )
        }
    }

    private suspend fun openCrateLocked(
        player: ServerPlayer,
        crate: Crate,
        openData: CrateOpenData,
        isForced: Boolean,
        ignoreInteractionLimiter: Boolean,
        ignoreCooldown: Boolean,
        forceInstantOpen: Boolean
    ): Boolean {
        var lockedWorldCratePos: DimensionalBlockPos? = null
        var openChargeContext: OpenChargeContext? = null

        fun releaseWorldCrateLock() {
            lockedWorldCratePos?.let {
                OpeningManager.unlockWorldCrate(it)
                lockedWorldCratePos = null
            }
        }

        try {
            if (!ignoreInteractionLimiter) {
                interactionLimiter[player.uuid]?.let {
                    if ((it + ConfigManager.CONFIG.interactionLimiter) > System.currentTimeMillis())
                        return false
                }

                interactionLimiter[player.uuid] = System.currentTimeMillis()
            }

            if (OpeningManager.getInstance(player.uuid) != null) {
                handleCrateFail(player, crate, openData)
                Lang.ERROR_ALREADY_OPENING.forEach {
                    player.sendMessage(TextUtils.parseAllNative(player, crate.parsePlaceholders(
                        it
                    )))
                }
                return false
            }

            if (!isForced && crate.permission.isNotEmpty() && !Permissions.check(player, crate.permission)) {
                handleCrateFail(player, crate, openData)
                Lang.ERROR_NO_PERMISSION.forEach {
                    player.sendMessage(TextUtils.parseAllNative(player, crate.parsePlaceholders(
                        it
                    )))
                }
                return false
            }

            if (!isForced && crate.inventorySpace > 0 && player.inventory.items.count { it.isEmpty } < crate.inventorySpace) {
                handleCrateFail(player, crate, openData)
                Lang.ERROR_INVENTORY_SPACE.forEach {
                    player.sendMessage(TextUtils.parseAllNative(player, crate.parsePlaceholders(
                        it
                    )))
                }
                return false
            }

            if (crate.cost != null && crate.cost.amount > 0) {
                val service = EconomyManager.getService(crate.cost.provider) ?: run {
                    handleCrateFail(player, crate, openData)
                    Utils.printError("Crate ${crate.id} has an invalid economy provider '${crate.cost.provider}'. Valid providers are: ${EconomyManager.getServices().keys.joinToString(", ")}")
                    Lang.ERROR_ECONOMY_PROVIDER.forEach {
                        player.sendMessage(TextUtils.parseAllNative(player, crate.parsePlaceholders(
                            it
                        )))
                    }
                    return false
                }
                if (service.balance(player, crate.cost.currency) < crate.cost.amount) {
                    handleCrateFail(player, crate, openData)
                    Lang.ERROR_COST.forEach {
                        player.sendMessage(TextUtils.parseAllNative(player, crate.parsePlaceholders(
                            it
                        )))
                    }
                    return false
                }
            }

            val storage = BiteCrates.INSTANCE.storage

            val playerData = storage.getUser(player.uuid)
            val chargeContext = OpenChargeContext(player, crate, playerData)
            openChargeContext = chargeContext

            if (!ignoreCooldown && crate.cooldown > 0) {
            val lastOpened = playerData.getCrateCooldown(crate)
            if (lastOpened != null) {
                val cooldownTime = lastOpened + (crate.cooldown * 1000)
                if (System.currentTimeMillis() < cooldownTime) {
                    withContext(MinecraftDispatcher(player.server)) {
                        handleCrateFail(player, crate, openData)
                        Lang.ERROR_COOLDOWN.forEach {
                            player.sendMessage(TextUtils.parseAllNative(player, crate.parsePlaceholders(
                                it.replace("%cooldown%", Utils.getFormattedTime((cooldownTime - System.currentTimeMillis()) / 1000))
                            )))
                        }
                    }
                    return false
                }
            }
        }

            if (crate.rewards.isEmpty()) {
            withContext(MinecraftDispatcher(player.server)) {
                handleCrateFail(player, crate, openData)
                Lang.ERROR_NO_REWARDS.forEach {
                    player.sendMessage(TextUtils.parseAllNative(player, crate.parsePlaceholders(it)))
                }
            }
            return false
        }

            if (!forceInstantOpen && crate.animation.isNotEmpty()) {
            val configuredAnimation = OpeningManager.getAnimation(crate.animation) ?: run {
                withContext(MinecraftDispatcher(player.server)) {
                    handleCrateFail(player, crate, openData)
                    Lang.ERROR_INVALID_ANIMATION.forEach {
                        player.sendMessage(TextUtils.parseAllNative(player, crate.parsePlaceholders(it)))
                    }
                }
                return false
            }

            if (configuredAnimation is WorldOpeningAnimation) {
                val positionData = openData.location ?: run {
                    withContext(MinecraftDispatcher(player.server)) {
                        handleCrateFail(player, crate, openData)
                        Utils.printError("No position data found for world opening animation ${crate.animation} for crate ${crate.id} for player ${player.name.string}!")
                        Lang.ERROR_INVALID_ANIMATION.forEach {
                            player.sendMessage(TextUtils.parseAllNative(player, crate.parsePlaceholders(it)))
                        }
                    }
                    return false
                }
                val crateInstance = getCrateFromPos(positionData) ?: run {
                    withContext(MinecraftDispatcher(player.server)) {
                        handleCrateFail(player, crate, openData)
                        Utils.printError("No crate instance found at $positionData for world opening animation ${crate.animation} for crate ${crate.id} for player ${player.name.string}!")
                        Lang.ERROR_INVALID_ANIMATION.forEach {
                            player.sendMessage(TextUtils.parseAllNative(player, crate.parsePlaceholders(it)))
                        }
                    }
                    return false
                }

                if (!OpeningManager.tryLockWorldCrate(crateInstance.dimPos)) {
                    withContext(MinecraftDispatcher(player.server)) {
                        handleCrateFail(player, crate, openData)
                        Lang.ERROR_CRATE_IN_USE.forEach {
                            player.sendMessage(TextUtils.parseAllNative(player, crate.parsePlaceholders(it)))
                        }
                    }
                    return false
                }

                lockedWorldCratePos = crateInstance.dimPos
            }
        }

            if (!isForced && crate.keys.isNotEmpty()) {
            if(!withContext(MinecraftDispatcher(player.server)) {
                if (!crate.keys.all { (keyId, amount) ->
                    val key = ConfigManager.KEYS[keyId] ?: run {
                        Utils.printError("Key $keyId does not exist while opening crate ${crate.id} for ${player.name.string}!")
                        Lang.ERROR_KEY_NOT_FOUND.forEach {
                            player.sendMessage(
                                TextUtils.parseAllNative(
                                    player, crate.parsePlaceholders(
                                        it.replace("%key_id%", keyId)
                                    )
                                )
                            )
                        }
                            return@all false
                        }

                        KeyManager.checkPlayerForKeys(player, playerData, key, amount)
                }) {
                    releaseWorldCrateLock()
                    handleCrateFail(player, crate, openData)
                    Lang.ERROR_MISSING_KEYS.forEach {
                        player.sendMessage(TextUtils.parseAllNative(player, crate.parsePlaceholders(
                            it
                        )))
                    }
                    return@withContext false
                }
                true
            }) return false
        }

            if (!isForced && openData.itemStack != null) {
            var contains = true
            withContext(MinecraftDispatcher(player.server)) {
                if (!player.inventory.contains { getCrateOrNull(it)?.id == crate.id }) {
                    contains = false
                    releaseWorldCrateLock()
                    handleCrateFail(player, crate, openData)
                    Lang.ERROR_NO_CRATE.forEach {
                        player.sendMessage(TextUtils.parseAllNative(player, crate.parsePlaceholders(
                            it
                        )))
                    }
                }
            }
            if (!contains) return false
        }

            if (!isForced) {
            if (crate.cost != null && crate.cost.amount > 0) {
                val service = EconomyManager.getService(crate.cost.provider) ?: run {
                    withContext(MinecraftDispatcher(player.server)) {
                        releaseWorldCrateLock()
                        handleCrateFail(player, crate, openData)
                        Utils.printError("Crate ${crate.id} has an invalid economy provider '${crate.cost.provider}'. Valid providers are: ${EconomyManager.getServices().keys.joinToString(", ")}")
                        Lang.ERROR_ECONOMY_PROVIDER.forEach {
                            player.sendMessage(TextUtils.parseAllNative(player, crate.parsePlaceholders(
                                it
                            )))
                        }
                    }
                    return false
                }
                if (!service.withdraw(player, crate.cost.amount, crate.cost.currency)) {
                    withContext(MinecraftDispatcher(player.server)) {
                        releaseWorldCrateLock()
                        handleCrateFail(player, crate, openData)
                        Lang.ERROR_BALANCE_CHANGED.forEach {
                            player.sendMessage(
                                TextUtils.parseAllNative(
                                    player, crate.parsePlaceholders(
                                        it
                                    )
                                )
                            )
                        }
                    }
                    return false
                }
                chargeContext.recordBalanceCharge(service, crate.cost.amount, crate.cost.currency)
            }

            if (crate.keys.isNotEmpty()) {
                if (!withContext(MinecraftDispatcher(player.server)) {
                    for ((keyId, amount) in crate.keys) {
                        var removed = 0
                        val key = ConfigManager.KEYS[keyId] ?: run {
                            releaseWorldCrateLock()
                            Utils.printError("Key $keyId does not exist while opening crate ${crate.id} for ${player.name.string}!")
                            Lang.ERROR_KEY_NOT_FOUND.forEach {
                                player.sendMessage(TextUtils.parseAllNative(player, crate.parsePlaceholders(
                                    it.replace("%key_id%", keyId)
                                )))
                            }
                            return@withContext false
                        }

                        if (key.virtual) {
                            if (!playerData.removeKeys(key, amount)) {
                                releaseWorldCrateLock()
                                Utils.printError("Failed to remove $amount keys from ${player.name.string} for crate ${crate.id}, but they were present in the check!")
                                Lang.ERROR_KEYS_CHANGED.forEach {
                                    player.sendMessage(TextUtils.parseAllNative(player, crate.parsePlaceholders(
                                        it.replace("%key_id%", keyId)
                                    )))
                                }
                                return@withContext false
                            }
                            chargeContext.recordVirtualKeyRemoval(key, amount)
                            removed += amount
                        } else {
                            for ((i, stack) in player.inventory.items.withIndex()) {
                                if (!stack.isEmpty) {
                                    if (KeyManager.getKeyOrNull(stack)?.id == keyId) {
                                        val stackSize = stack.count
                                        if (removed + stackSize >= amount) {
                                            val removedAmount = amount - removed
                                            val removedStack = stack.copy()
                                            removedStack.count = removedAmount
                                            KeyManager.markStackUsed(stack, key, keyId, player)
                                            player.inventory.items[i].shrink(removedAmount)
                                            chargeContext.recordItemRemoval(removedStack)
                                            removed += removedAmount
                                            break
                                        } else {
                                            val removedStack = stack.copy()
                                            removedStack.count = stackSize
                                            KeyManager.markStackUsed(stack, key, keyId, player)
                                            player.inventory.items[i].shrink(stackSize)
                                            chargeContext.recordItemRemoval(removedStack)
                                            removed += stackSize
                                        }
                                    }
                                }
                            }
                        }

                        if (removed != amount) {
                            releaseWorldCrateLock()
                            Utils.printError("Somehow the ${player.name.string} had $amount keys on check, but we removed $removed instead!")
                            Lang.ERROR_KEYS_CHANGED.forEach {
                                player.sendMessage(TextUtils.parseAllNative(player, crate.parsePlaceholders(
                                    it.replace("%key_id%", keyId)
                                )))
                            }
                            return@withContext false
                        }
                    }
                    return@withContext true
                }) {
                    releaseWorldCrateLock()
                    chargeContext.refund()
                    return false
                }
            }

            if (openData.itemStack != null) {
                withContext(MinecraftDispatcher(player.server)) {
                    val consumedCrate = openData.itemStack.copy()
                    consumedCrate.count = 1
                    openData.itemStack.count -= 1
                    chargeContext.recordItemRemoval(consumedCrate)
                }
            }
        }

            return withContext(MinecraftDispatcher(player.server)) {
            if (!chargeContext.persistDeductions()) {
                releaseWorldCrateLock()
                return@withContext false
            }

            Lang.CRATE_OPENING.forEach {
                player.sendMessage(TextUtils.parseAllNative(player, crate.parsePlaceholders(
                    it
                )))
            }

            val rewardBag = crate.generateRewardBag(playerData)
            if (rewardBag.size() <= 0) {
                releaseWorldCrateLock()
                chargeContext.refund()
                handleCrateFail(player, crate, openData)
                Lang.ERROR_NO_REWARDS.forEach {
                    player.sendMessage(TextUtils.parseAllNative(player, crate.parsePlaceholders(
                        it
                    )))
                }
                return@withContext false
            }

            if (forceInstantOpen || crate.animation.isEmpty()) {
                val reward = rewardBag.next() ?: run {
                    releaseWorldCrateLock()
                    chargeContext.refund()
                    handleCrateFail(player, crate, openData)
                    Lang.ERROR_NO_REWARDS.forEach {
                        player.sendMessage(TextUtils.parseAllNative(player, crate.parsePlaceholders(
                            it
                        )))
                    }
                    return@withContext false
                }
                if (!reward.giveReward(player, crate)) {
                    releaseWorldCrateLock()
                    chargeContext.refund()
                    handleCrateFail(player, crate, openData)
                    return@withContext false
                }
                releaseWorldCrateLock()
                return@withContext chargeContext.complete(listOf(reward))
            }

            val animation = OpeningManager.getAnimation(crate.animation) ?: run {
                releaseWorldCrateLock()
                chargeContext.refund()
                handleCrateFail(player, crate, openData)
                Lang.ERROR_INVALID_ANIMATION.forEach {
                    player.sendMessage(TextUtils.parseAllNative(player, crate.parsePlaceholders(
                        it
                    )))
                }
                return@withContext false
            }

            val opening = when (animation) {
                is InventoryOpeningAnimation -> {
                    InventoryOpeningInstance(player, crate, chargeContext, animation, rewardBag)
                }
                is WorldOpeningAnimation -> {
                    val positionData = openData.location ?: run {
                        releaseWorldCrateLock()
                        chargeContext.refund()
                        handleCrateFail(player, crate, openData)
                        Utils.printError("No position data found for world opening animation ${crate.animation} for crate ${crate.id} for player ${player.name.string}!")
                        Lang.ERROR_INVALID_ANIMATION.forEach {
                            player.sendMessage(TextUtils.parseAllNative(player, crate.parsePlaceholders(
                                it
                            )))
                        }
                        return@withContext false
                    }
                    val crateInstance = getCrateFromPos(positionData) ?: run {
                        releaseWorldCrateLock()
                        chargeContext.refund()
                        handleCrateFail(player, crate, openData)
                        Utils.printError("No crate instance found at $positionData for world opening animation ${crate.animation} for crate ${crate.id} for player ${player.name.string}!")
                        Lang.ERROR_INVALID_ANIMATION.forEach {
                            player.sendMessage(TextUtils.parseAllNative(player, crate.parsePlaceholders(
                                it
                            )))
                        }
                        return@withContext false
                    }
                    WorldOpeningInstance(player, crate, chargeContext, crateInstance, animation, rewardBag)
                }
                else -> {
                    releaseWorldCrateLock()
                    chargeContext.refund()
                    handleCrateFail(player, crate, openData)
                    Lang.ERROR_INVALID_ANIMATION.forEach {
                        player.sendMessage(TextUtils.parseAllNative(player, crate.parsePlaceholders(
                            it
                        )))
                    }
                    return@withContext false
                }
            }

            OpeningManager.addInstance(player.uuid, opening)
            opening.setup()

            return@withContext true
        }
        } catch (ex: Exception) {
            if (ex is CancellationException) {
                throw ex
            }

            releaseWorldCrateLock()
            runCatching { openChargeContext?.refund() }

            Utils.printError("Unexpected error while opening crate ${crate.id} for ${player.name.string}.")
            BiteCrates.LOGGER.error("Unexpected error while opening crate {} for {}", crate.id, player.name.string, ex)

            withContext(MinecraftDispatcher(player.server)) {
                handleCrateFail(player, crate, openData)
                Lang.ERROR_OPENING.forEach {
                    player.sendMessage(TextUtils.parseAllNative(player, crate.parsePlaceholders(it)))
                }
            }
            return false
        }
    }

    private suspend fun openAllCratesWithKeys(player: ServerPlayer, crate: Crate, openData: CrateOpenData): Boolean {
        if (crate.keys.isEmpty()) {
            return openCrate(player, crate, openData, false)
        }

        if (OpeningManager.getInstance(player.uuid) != null) {
            withContext(MinecraftDispatcher(player.server)) {
                handleCrateFail(player, crate, openData)
                Lang.ERROR_ALREADY_OPENING.forEach {
                    player.sendMessage(TextUtils.parseAllNative(player, crate.parsePlaceholders(it)))
                }
            }
            return false
        }

        val storage = BiteCrates.INSTANCE.storage
        val playerData = storage.getUser(player.uuid)

        val maxByKeys = withContext(MinecraftDispatcher(player.server)) {
            getMaxOpenCountByKeys(player, playerData, crate)
        } ?: return false

        if (maxByKeys <= 0) {
            withContext(MinecraftDispatcher(player.server)) {
                handleCrateFail(player, crate, openData)
                Lang.ERROR_MISSING_KEYS.forEach {
                    player.sendMessage(TextUtils.parseAllNative(player, crate.parsePlaceholders(it)))
                }
            }
            return false
        }

        var totalToOpen = maxByKeys
        if (crate.cost != null && crate.cost.amount > 0) {
            val service = EconomyManager.getService(crate.cost.provider) ?: run {
                withContext(MinecraftDispatcher(player.server)) {
                    handleCrateFail(player, crate, openData)
                    Utils.printError("Crate ${crate.id} has an invalid economy provider '${crate.cost.provider}'. Valid providers are: ${EconomyManager.getServices().keys.joinToString(", ")}")
                    Lang.ERROR_ECONOMY_PROVIDER.forEach {
                        player.sendMessage(TextUtils.parseAllNative(player, crate.parsePlaceholders(it)))
                    }
                }
                return false
            }
            val affordable = (service.balance(player, crate.cost.currency) / crate.cost.amount).toInt()
            totalToOpen = min(totalToOpen, affordable)

            if (totalToOpen <= 0) {
                withContext(MinecraftDispatcher(player.server)) {
                    handleCrateFail(player, crate, openData)
                    Lang.ERROR_COST.forEach {
                        player.sendMessage(TextUtils.parseAllNative(player, crate.parsePlaceholders(it)))
                    }
                }
                return false
            }
        }

        if (crate.inventorySpace > 0) {
            val availableSlots = withContext(MinecraftDispatcher(player.server)) {
                player.inventory.items.count { it.isEmpty }
            }
            val requiredSlots = crate.inventorySpace.toLong() * totalToOpen.toLong()
            if (availableSlots.toLong() < requiredSlots) {
                withContext(MinecraftDispatcher(player.server)) {
                    handleCrateFail(player, crate, openData)
                    Lang.ERROR_INVENTORY_SPACE_BULK.forEach { message ->
                        player.sendMessage(
                            TextUtils.parseAllNative(
                                player,
                                crate.parsePlaceholders(
                                    message
                                        .replace("%required_space%", requiredSlots.toString())
                                        .replace("%available_space%", availableSlots.toString())
                                        .replace("%open_amount%", totalToOpen.toString())
                                )
                            )
                        )
                    }
                }
                return false
            }
        }

        withContext(MinecraftDispatcher(player.server)) {
            Lang.CRATE_OPENING_ALL.forEach {
                player.sendMessage(
                    TextUtils.parseAllNative(
                        player,
                        crate.parsePlaceholders(it.replace("%open_amount%", totalToOpen.toString()))
                    )
                )
            }
        }

        var openedCount = 0
        for (openIndex in 0 until totalToOpen) {
            val opened = openCrate(
                player,
                crate,
                openData,
                false,
                ignoreInteractionLimiter = true,
                ignoreCooldown = true,
                forceInstantOpen = true
            )
            if (!opened) {
                break
            }
            openedCount++
        }

        return openedCount > 0
    }

    private fun getMaxOpenCountByKeys(
        player: ServerPlayer,
        playerData: com.dawnshade.biteforce.bitecrates.state.userdata.UserData,
        crate: Crate
    ): Int? {
        var maxOpenCount = Int.MAX_VALUE
        for ((keyId, requiredAmount) in crate.keys) {
            if (requiredAmount <= 0) continue

            val key = ConfigManager.KEYS[keyId] ?: run {
                Utils.printError("Key $keyId does not exist while bulk opening crate ${crate.id} for ${player.name.string}!")
                Lang.ERROR_KEY_NOT_FOUND.forEach {
                    player.sendMessage(TextUtils.parseAllNative(player, crate.parsePlaceholders(it.replace("%key_id%", keyId))))
                }
                return null
            }

            val available = if (key.virtual) {
                playerData.keys[key.id] ?: 0
            } else {
                val rawCount = player.inventory.items
                    .filter { stack -> !stack.isEmpty && KeyManager.getKeyOrNull(stack)?.id == keyId }
                    .sumOf { stack -> stack.count }
                if (rawCount <= 0) {
                    0
                } else {
                    var low = 0
                    var high = rawCount
                    while (low < high) {
                        val mid = (low + high + 1) / 2
                        if (KeyManager.checkPlayerForKeys(player, playerData, key, mid)) {
                            low = mid
                        } else {
                            high = mid - 1
                        }
                    }
                    low
                }
            }

            maxOpenCount = min(maxOpenCount, available / requiredAmount)
            if (maxOpenCount <= 0) {
                return 0
            }
        }

        return if (maxOpenCount == Int.MAX_VALUE) 0 else maxOpenCount
    }

    fun previewCrate(player: ServerPlayer, crate: Crate) {
        interactionLimiter[player.uuid]?.let {
            if ((it + ConfigManager.CONFIG.interactionLimiter) > System.currentTimeMillis()) {
                return
            }
        }

        interactionLimiter[player.uuid] = System.currentTimeMillis()

        val preview = ConfigManager.PREVIEW[crate.preview] ?: run {
            Lang.ERROR_INVALID_PREVIEW.forEach {
                player.sendMessage(TextUtils.parseAllNative(player, crate.parsePlaceholders(
                    it
                )))
            }
            return
        }
        Utils.printDebug("previewCrate - Preview found, opening")

        PreviewInventory(player, crate, preview).open()
    }

    private fun handleCrateFail(player: ServerPlayer, crate: Crate, openData: CrateOpenData) {
        crate.failure?.sound?.playSound(player)
        val force = crate.failure?.pushback ?: return
        if (openData.location != null) {
            val blockPos = openData.location.getBlockPos()
            val sourcePos = blockPos.center
            val playerPos = player.position()

            val direction = Vec3(
                playerPos.x - sourcePos.x,
                playerPos.y - sourcePos.y,
                playerPos.z - sourcePos.z
            ).normalize()

            val velocity = direction.scale(force)
            player.addDeltaMovement(velocity)
            player.hurtMarked = true
        }
    }

    fun getCrateOrNull(itemStack: ItemStack): Crate? {
        val tag = itemStack.get(DataComponents.CUSTOM_DATA) ?: return null
        if (tag.contains(CRATE_IDENTIFIER)) {
            return ConfigManager.CRATES[tag.copyTag().getString(CRATE_IDENTIFIER)]
        }
        return null
    }

    private fun getChunkInstances(key: ChunkKey): MutableMap<DimensionalBlockPos, CrateInstance> =
        instancesByChunk.computeIfAbsent(key) { mutableMapOf() }

    fun getCrateFromPos(pos: DimensionalBlockPos): CrateInstance? {
        val key = ChunkKey.of(pos)
        val chunkInstances = instancesByChunk[key] ?: return null
        return chunkInstances[pos]
    }

    fun getInstancesAtChunk(level: ServerLevel, chunk: LevelChunk): List<CrateInstance> {
        val key = ChunkKey.of(level, chunk)
        val chunkInstances = instancesByChunk[key] ?: return emptyList()

        return chunkInstances.values.toList()
    }

    fun getAllInstances(): List<CrateInstance> {
        return instancesByChunk.values.flatMap { instances ->
            instances.values.toList()
        }
    }
}
