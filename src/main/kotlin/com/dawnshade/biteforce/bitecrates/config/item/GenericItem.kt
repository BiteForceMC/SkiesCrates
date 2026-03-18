package com.dawnshade.biteforce.bitecrates.config.item

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.mojang.authlib.properties.Property
import com.mojang.authlib.properties.PropertyMap
import com.dawnshade.biteforce.bitecrates.core.BiteCrates
import com.dawnshade.biteforce.bitecrates.placeholders.PlaceholderManager
import com.dawnshade.biteforce.bitecrates.util.FlexibleListAdaptorFactory
import com.dawnshade.biteforce.bitecrates.util.PokemonDisplayUtils
import com.dawnshade.biteforce.bitecrates.util.TextUtils
import com.dawnshade.biteforce.bitecrates.util.Utils
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.CustomModelData
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.item.component.ResolvableProfile
import java.util.*

open class GenericItem(
    val item: String = "",
    val amount: Int = 1,
    var name: String? = null,
    @JsonAdapter(FlexibleListAdaptorFactory::class)
    val lore: List<String>? = null,
    @SerializedName("components", alternate = ["nbt"])
    val components: CompoundTag? = null,
    @SerializedName("custom_model_data")
    val customModelData: Int? = null,
    val species: String? = null,
    @JsonAdapter(FlexibleListAdaptorFactory::class)
    @SerializedName(value = "aspects", alternate = ["aspect"])
    val aspects: List<String> = emptyList(),
) {
    fun createItemStack(
        player: ServerPlayer,
        placeholders: Map<String, String> = emptyMap(),
        fallbackName: String? = null
    ): ItemStack {
        val stack = getBaseItem(player, placeholders) ?: return ItemStack(Items.AIR, amount)

        if (components != null) {
            
            val nbtCopy = components.copy()
            for (key in components.allKeys) {
                val element = components.get(key)
                if (element != null) {
                    if (element is StringTag) {
                        nbtCopy.putString(key, element.asString)
                    } else if (element is ListTag) {
                        val parsed = ListTag()
                        for (entry in element) {
                            if (entry is StringTag) {
                                parsed.add(StringTag.valueOf(entry.asString))
                            } else {
                                parsed.add(entry)
                            }
                        }
                        nbtCopy.put(key, parsed)
                    }
                }
            }

            DataComponentPatch.CODEC.decode(BiteCrates.INSTANCE.nbtOpts, nbtCopy).result().ifPresent { result ->
                stack.applyComponents(result.first)
            }
        }

        val dataComponents = DataComponentPatch.builder()

        if (customModelData != null) {
            dataComponents.set(DataComponents.CUSTOM_MODEL_DATA, CustomModelData(customModelData))
        }

        (name ?: fallbackName)?.let { name ->
            dataComponents.set(
                DataComponents.ITEM_NAME, Component.empty().setStyle(Style.EMPTY.withItalic(false))
                    .append(TextUtils.parseAllNative(player, name, placeholders)))
        }

        if (!lore.isNullOrEmpty()) {
            val parsedLore: MutableList<String> = mutableListOf()
            for (line in lore.stream().map { it }.toList()) {
                val parsedLine = PlaceholderManager.parse(player, line, placeholders)
                if (parsedLine.contains("\n")) {
                    parsedLine.split("\n").forEach { parsedLore.add(it) }
                } else {
                    parsedLore.add(parsedLine)
                }
            }
            dataComponents.set(DataComponents.LORE, ItemLore(parsedLore.stream().map {
                Component.empty().setStyle(Style.EMPTY.withItalic(false)).append(
                    TextUtils.toNative(
                        it
                    )
                ) as Component
            }.toList()))
        }

        stack.applyComponents(dataComponents.build())

        return stack
    }

    private fun getBaseItem(player: ServerPlayer, placeholders: Map<String, String> = emptyMap()): ItemStack? {
        if (item.isEmpty()) return null

        val parsedItem = PlaceholderManager.parse(player, item, placeholders)

        if (parsedItem.equals("cobblemon:pokemon_model", ignoreCase = true)) {
            val parsedSpecies = species?.let { PlaceholderManager.parse(player, it, placeholders) }
            val parsedAspects = aspects.map { PlaceholderManager.parse(player, it, placeholders) }
            return PokemonDisplayUtils.createPokemonModelItem(parsedSpecies, parsedAspects)
        }

        
        if (parsedItem.startsWith("playerhead", true)) {
            val headStack = ItemStack(Items.PLAYER_HEAD, amount)

            var uuid: UUID? = null
            if (parsedItem.contains("-")) {
                val arg = parsedItem.replace("playerhead-", "")
                if (arg.isNotEmpty()) {
                    if (arg.contains("-")) {
                        
                        try {
                            uuid = UUID.fromString(arg)
                        } catch (_: Exception) {}
                    } else if (arg.length <= 16) {
                        
                        val targetPlayer = BiteCrates.INSTANCE.server.playerList?.getPlayerByName(arg)
                        if (targetPlayer != null) {
                            uuid = targetPlayer.uuid
                        }
                    } else {
                        
                        val properties = PropertyMap()
                        properties.put("textures", Property("textures", arg))
                        headStack.applyComponents(DataComponentPatch.builder()
                            .set(DataComponents.PROFILE, ResolvableProfile(Optional.empty(), Optional.empty(), properties))
                            .build())
                        return headStack
                    }
                }
            } else {
                
                uuid = player.uuid
            }

            if (uuid != null) {
                val gameProfile = BiteCrates.INSTANCE.server.profileCache?.get(uuid)
                if (gameProfile != null && gameProfile.isPresent) {
                    headStack.applyComponents(DataComponentPatch.builder()
                        .set(DataComponents.PROFILE, ResolvableProfile(gameProfile.get()))
                        .build())
                    return headStack
                }
            }

            Utils.printError("Error while attempting to parse Player Head: $parsedItem")
            return headStack
        }

        val newItem = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(parsedItem))
        if (newItem.isEmpty) {
            Utils.printError("Error while getting Item, defaulting to AIR: $parsedItem")
            return ItemStack(Items.AIR, amount)
        }

        return ItemStack(newItem.get(), amount)
    }

    override fun toString(): String {
        return "GenericItem(item=$item, amount=$amount, name=$name, lore=$lore, components=$components, customModelData=$customModelData, species=$species, aspects=$aspects)"
    }
}
