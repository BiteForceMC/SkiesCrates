package com.dawnshade.biteforce.bitecrates.feature.reward.types

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.item.PokemonItem
import com.cobblemon.mod.common.pokemon.Pokemon
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.dawnshade.biteforce.bitecrates.config.item.GenericItem
import com.dawnshade.biteforce.bitecrates.data.Crate
import com.dawnshade.biteforce.bitecrates.feature.reward.Reward
import com.dawnshade.biteforce.bitecrates.feature.reward.RewardLimits
import com.dawnshade.biteforce.bitecrates.feature.reward.RewardType
import com.dawnshade.biteforce.bitecrates.feature.reward.options.bool.BooleanChance
import com.dawnshade.biteforce.bitecrates.feature.reward.options.bool.BooleanOption
import com.dawnshade.biteforce.bitecrates.feature.reward.options.bool.BooleanValue
import com.dawnshade.biteforce.bitecrates.feature.reward.options.int.IntOption
import com.dawnshade.biteforce.bitecrates.placeholders.PlaceholderManager
import com.dawnshade.biteforce.bitecrates.util.FlexibleListAdaptorFactory
import com.dawnshade.biteforce.bitecrates.util.PokemonDisplayUtils
import com.dawnshade.biteforce.bitecrates.util.TextUtils
import com.dawnshade.biteforce.bitecrates.util.Utils
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemLore

class PokemonReward(
    name: String = "",
    display: GenericItem? = null,
    weight: Int = 1,
    limits: RewardLimits? = null,
    broadcast: Boolean = false,
    private val pokemon: PokemonRewardOptions = PokemonRewardOptions(),
) : Reward(RewardType.POKEMON, name, display, weight, limits, broadcast) {
    companion object {
        private val DEFAULT_DISPLAY = GenericItem("cobblemon:poke_ball", name = "Pokemon Reward")
    }

    override fun giveReward(player: ServerPlayer, crate: Crate): Boolean {
        if (!super.giveReward(player, crate)) {
            return false
        }
        val pokemonInstance = pokemon.createPokemon(false) ?: run {
            Utils.printError("Failed to create Pokemon for reward '$name' when giving to player ${player.name}.")
            player.sendMessage(net.kyori.adventure.text.Component.text("Your Pokemon Reward could not be created! Please contact an administrator.",
                NamedTextColor.RED))
            return false
        }

        if (!Cobblemon.storage.getParty(player).add(pokemonInstance)) {
            Utils.printInfo("There was an error while giving a player their Pokemon Reward (${pokemon}).")
            player.sendMessage(net.kyori.adventure.text.Component.text("Your Pokemon Reward could not be added to your party! Please contact an administrator.",
                NamedTextColor.RED))
            return false
        }
        return true
    }

    override fun getGenericDisplay(): GenericItem {
        return display ?: DEFAULT_DISPLAY
    }

    override fun getDisplayItem(player: ServerPlayer, placeholders: Map<String, String>): ItemStack {
        if (display != null && display.item.isNotEmpty() && !display.item.equals("cobblemon:pokemon_model", ignoreCase = true)) {
            return display.also {
                if (it.name == null) it.name = name
            }.createItemStack(player, placeholders)
        }

        val pokemonInstance = pokemon.createPokemon(true)
        val itemStack = if (pokemonInstance != null && !pokemon.isRandom()) {
            PokemonItem.from(pokemonInstance)
        } else {
            DEFAULT_DISPLAY.createItemStack(player, placeholders)
        }

        DataComponentPatch.builder().let {
            it.set(DataComponents.ITEM_NAME, Component.empty().setStyle(Style.EMPTY.withItalic(false))
                    .append(TextUtils.parseAllNative(player, name, placeholders)))
            itemStack.applyComponents(it.build())
        }

        display?.let {
            val dataComponents = DataComponentPatch.builder()

            if (display.name != null) {
                dataComponents.set(
                    DataComponents.ITEM_NAME, Component.empty().setStyle(Style.EMPTY.withItalic(false))
                        .append(TextUtils.parseAllNative(player, name, placeholders)))
            }

            if (!display.lore.isNullOrEmpty()) {
                val parsedLore: MutableList<String> = mutableListOf()
                for (line in display.lore.stream().map { it }.toList()) {
                    val parsedLine = PlaceholderManager.parse(player, line)
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

            itemStack.applyComponents(dataComponents.build())

            itemStack
        }

        return itemStack
    }

    class PokemonRewardOptions(
        val species: String = "",
        val form: String = "",
        @JsonAdapter(FlexibleListAdaptorFactory::class)
        @SerializedName("aspects", alternate = ["aspect"])
        val aspects: List<String> = emptyList(),
        val level: IntOption? = null,
        val shiny: BooleanOption? = null,
    ) {
        fun isRandom(): Boolean {
            return species.equals("random", ignoreCase = true)
        }

        fun createPokemon(isDisplay: Boolean): Pokemon? {
            if (species.isEmpty()) {
                Utils.printError("Species was empty when creating Pokemon reward.")
                return null
            }
            val species = if (isRandom()) {
                PokemonSpecies.random()
            } else {
                if (species.contains(":")) {
                    ResourceLocation.tryParse(species)?.let {
                        PokemonSpecies.getByIdentifier(it)
                    }
                } else {
                    PokemonSpecies.getByName(species)
                }
            } ?: run {
                Utils.printError("Could not find Pokemon species '$species' when creating Pokemon reward.")
                return null
            }

            val resolvedShiny = when (shiny) {
                is BooleanValue -> shiny.bool
                is BooleanChance -> if (!isDisplay) shiny.getValue() else null
                else -> null
            }

            return PokemonDisplayUtils.createPokemon(
                species,
                level?.getValue() ?: 1,
                aspects,
                form.takeIf { it.isNotBlank() },
                resolvedShiny
            )
        }
    }

    override fun toString(): String {
        return "PokemonReward(name='$name', display=$display, weight=$weight, limits=$limits, broadcast=$broadcast, pokemon=$pokemon)"
    }
}
