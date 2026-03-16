package com.dawnshade.biteforce.bitecrates.core

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.dawnshade.biteforce.bitecrates.commands.BaseCommand
import com.dawnshade.biteforce.bitecrates.commands.KeysCommand
import com.dawnshade.biteforce.bitecrates.config.ConfigManager
import com.dawnshade.biteforce.bitecrates.config.Lang
import com.dawnshade.biteforce.bitecrates.config.SoundOption
import com.dawnshade.biteforce.bitecrates.data.actions.Action
import com.dawnshade.biteforce.bitecrates.feature.opening.world.WorldOpeningAnimation
import com.dawnshade.biteforce.bitecrates.feature.opening.world.types.CarouselWorldAnimation
import com.dawnshade.biteforce.bitecrates.feature.particle.effects.ParticleEffect
import com.dawnshade.biteforce.bitecrates.feature.reward.Reward
import com.dawnshade.biteforce.bitecrates.feature.reward.options.bool.BooleanOption
import com.dawnshade.biteforce.bitecrates.feature.reward.options.int.IntOption
import com.dawnshade.biteforce.bitecrates.economy.EconomyManager
import com.dawnshade.biteforce.bitecrates.gui.InventoryType
import com.dawnshade.biteforce.bitecrates.integrations.ModIntegration
import com.dawnshade.biteforce.bitecrates.core.CratesManager
import com.dawnshade.biteforce.bitecrates.core.CratesManager.tick
import com.dawnshade.biteforce.bitecrates.core.HologramsManager
import com.dawnshade.biteforce.bitecrates.core.KeyManager
import com.dawnshade.biteforce.bitecrates.core.OpeningManager
import com.dawnshade.biteforce.bitecrates.placeholders.PlaceholderManager
import com.dawnshade.biteforce.bitecrates.storage.IStorage
import com.dawnshade.biteforce.bitecrates.storage.StorageType
import com.dawnshade.biteforce.bitecrates.util.CompoundTagAdaptor
import com.dawnshade.biteforce.bitecrates.util.Utils
import kotlinx.coroutines.*
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.loader.api.FabricLoader
import net.kyori.adventure.platform.fabric.FabricServerAudiences
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.Tag
import net.minecraft.resources.RegistryOps
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.item.Item
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BiteCrates : ModInitializer {
    companion object {
        lateinit var INSTANCE: BiteCrates

        const val MOD_ID = "bitecrates"
        const val MOD_NAME = "BiteCrates"

        val LOGGER: Logger = LoggerFactory.getLogger(BiteCrates::class.java)
        val MINI_MESSAGE: MiniMessage = MiniMessage.miniMessage()

        val asyncScope = CoroutineScope(Dispatchers.IO)

        @JvmStatic
        fun asResource(path: String): ResourceLocation {
            return ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
        }
    }

    lateinit var configDir: File
    lateinit var storage: IStorage

    lateinit var adventure: FabricServerAudiences
    lateinit var server: MinecraftServer
    lateinit var nbtOpts: RegistryOps<Tag>

    val asyncExecutor: ExecutorService = Executors.newFixedThreadPool(8, ThreadFactoryBuilder()
        .setNameFormat("BiteCrates-Async-%d")
        .setDaemon(true)
        .build())

    @OptIn(DelicateCoroutinesApi::class)
    private val particleThreadPool = newFixedThreadPoolContext(1, "BiteCratesParticleThread")
    private val particleScope = CoroutineScope(particleThreadPool + SupervisorJob())
    fun runOnParticleThread(block: suspend () -> Unit) {
        particleScope.launch {
            block()
            delay(1)
        }
    }

    var gson: Gson = GsonBuilder().disableHtmlEscaping()
        .registerTypeAdapter(Reward::class.java, Reward.Adapter())
        .registerTypeAdapter(Action::class.java, Action.Adapter())
        .registerTypeAdapter(StorageType::class.java, StorageType.Adapter())
        .registerTypeAdapter(ParticleEffect::class.java, ParticleEffect.Adapter())
        .registerTypeAdapter(WorldOpeningAnimation::class.java, WorldOpeningAnimation.Adapter())
        .registerTypeAdapter(IntOption::class.java, IntOption.Adapter())
        .registerTypeAdapter(BooleanOption::class.java, BooleanOption.Adapter())
        .registerTypeHierarchyAdapter(Item::class.java, Utils.RegistrySerializer(BuiltInRegistries.ITEM))
        .registerTypeHierarchyAdapter(SoundEvent::class.java, Utils.RegistrySerializer(BuiltInRegistries.SOUND_EVENT))
        .registerTypeHierarchyAdapter(ParticleOptions::class.java, Utils.CodecSerializer(ParticleTypes.CODEC))
        .registerTypeAdapter(InventoryType::class.java, InventoryType.Deserializer())
        .registerTypeAdapter(SoundOption::class.java, SoundOption.Adaptor())
        .registerTypeAdapter(CompoundTag::class.java, CompoundTagAdaptor())
        .create()

    var gsonPretty: Gson = gson.newBuilder().setPrettyPrinting().create()

    override fun onInitialize() {
        INSTANCE = this

        this.configDir = File(FabricLoader.getInstance().configDirectory, MOD_ID)
        ConfigManager.load()
        this.storage = IStorage.load(ConfigManager.CONFIG.storage)
        Lang.init()

        EconomyManager.init()

        ModIntegration.onInit()

        registerEvents()
    }

    private fun registerEvents() {
        ServerLifecycleEvents.SERVER_STARTING.register(ServerLifecycleEvents.ServerStarting { server: MinecraftServer ->
            this.adventure = FabricServerAudiences.of(server)
            this.server = server
            this.nbtOpts = server.registryAccess().createSerializationContext(NbtOps.INSTANCE)
            ModIntegration.onServerStarting()
        })
        ServerLifecycleEvents.SERVER_STARTED.register(ServerLifecycleEvents.ServerStarted { _: MinecraftServer ->
            CarouselWorldAnimation.cleanupOrphanedDisplays(this.server)
            OpeningManager.load()
            CratesManager.init()
            PlaceholderManager.init()
            ModIntegration.onServerStarted()
        })
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            BaseCommand().register(dispatcher)
            KeysCommand().register(dispatcher)
        }
        ServerLifecycleEvents.SERVER_STOPPING.register(ServerLifecycleEvents.ServerStopping { _: MinecraftServer ->
            OpeningManager.stopAll()
            CarouselWorldAnimation.cleanupOrphanedDisplays(this.server)
            this.storage.close()
            ModIntegration.onServerShutdown()
        })
        ServerTickEvents.END_SERVER_TICK.register(ServerTickEvents.EndTick { server ->
            tick()
            OpeningManager.tick()

            if (server.tickCount % 6000 == 0) {
                KeyManager.cleanCache()
            }
        })
    }

    fun reload() {
        OpeningManager.stopAll()
        CarouselWorldAnimation.cleanupOrphanedDisplays(this.server)
        this.storage.close()

        ConfigManager.load()
        this.storage = IStorage.load(ConfigManager.CONFIG.storage)
        Lang.init()

        OpeningManager.load()
        CarouselWorldAnimation.cleanupOrphanedDisplays(this.server)
        CratesManager.init()

        if (FabricLoader.getInstance().isModLoaded("holodisplays")) HologramsManager.load()
    }
}
