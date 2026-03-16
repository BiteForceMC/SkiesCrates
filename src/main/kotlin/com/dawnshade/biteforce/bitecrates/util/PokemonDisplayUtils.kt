package com.dawnshade.biteforce.bitecrates.util

import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.item.PokemonItem
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.pokemon.Species
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.slf4j.LoggerFactory
import java.util.LinkedHashSet
import java.util.Locale

object PokemonDisplayUtils {
    private val LOGGER = LoggerFactory.getLogger(PokemonDisplayUtils::class.java)

    fun createPokemonModelItem(speciesId: String?, aspects: List<String>, level: Int = 1): ItemStack {
        val pokemon = createPokemon(speciesId, level, aspects)
        if (pokemon != null) {
            return PokemonItem.from(pokemon)
        }

        val species = resolveSpecies(speciesId)
        return if (species == null) {
            ItemStack(Items.NETHER_STAR)
        } else {
            PokemonItem.from(species.create(level.coerceAtLeast(1)))
        }
    }

    fun createPokemon(
        speciesId: String?,
        level: Int,
        aspects: List<String>,
        form: String? = null,
        shiny: Boolean? = null
    ): Pokemon? {
        val species = resolveSpecies(speciesId) ?: return null
        return createPokemon(species, level, aspects, form, shiny)
    }

    fun createPokemon(
        species: Species,
        level: Int,
        aspects: List<String>,
        form: String? = null,
        shiny: Boolean? = null
    ): Pokemon? {
        val configured = tryCreateConfiguredPokemon(species, level, aspects, form, shiny)
        if (configured != null) {
            applyAspects(configured, aspects)
            return configured
        }

        val pokemon = species.create(level.coerceAtLeast(1))
        if (!form.isNullOrBlank()) {
            pokemon.form = species.getFormByName(form)
        }
        applyAspects(pokemon, aspects)
        shiny?.let { pokemon.shiny = it }
        pokemon.initialize()
        return pokemon
    }

    fun resolveSpecies(speciesId: String?): Species? {
        val normalized = normalizeSpeciesId(speciesId) ?: return null
        val byName = PokemonSpecies.getByName(normalized)
        if (byName != null) {
            return byName
        }

        val resourceLocation = ResourceLocation.tryParse(
            if (normalized.contains(":")) normalized else "cobblemon:$normalized"
        ) ?: return null
        return PokemonSpecies.getByIdentifier(resourceLocation)
    }

    private fun applyAspects(pokemon: Pokemon, aspects: List<String>) {
        val forcedAspects = LinkedHashSet(pokemon.forcedAspects)
        val actualAspects = LinkedHashSet(pokemon.aspects)
        var changed = false

        for (raw in aspects) {
            if (raw.isBlank()) {
                continue
            }

            var applied = false
            for (property in aspectCandidates(raw)) {
                try {
                    val parsed = PokemonProperties.parse(property)
                    if (!isMeaningfulAspect(parsed, property)) {
                        continue
                    }

                    parsed.apply(pokemon)
                    forcedAspects.addAll(parsed.aspects)
                    actualAspects.addAll(parsed.aspects)
                    changed = true
                    applied = true
                    break
                } catch (exception: Exception) {
                    LOGGER.debug("Failed to apply aspect '{}' as '{}'", raw, property, exception)
                }
            }

            if (!applied) {
                val customAspect = customAspectToken(raw)
                forcedAspects.add(customAspect)
                actualAspects.add(customAspect)
                changed = true
                LOGGER.debug("Force-injected custom aspect '{}' for {}", customAspect, pokemon.species.name)
            }
        }

        if (changed) {
            pokemon.forcedAspects = forcedAspects
            replaceAspects(pokemon, actualAspects)
            try {
                pokemon.updateAspects()
            } catch (exception: Exception) {
                LOGGER.debug("updateAspects threw for {}", pokemon.species.name, exception)
            }
            mergeAspects(pokemon, actualAspects)
            try {
                pokemon.updateForm()
            } catch (exception: Exception) {
                LOGGER.debug("updateForm threw for {}", pokemon.species.name, exception)
            }
            mergeAspects(pokemon, actualAspects)
        }
    }

    private fun tryCreateConfiguredPokemon(
        species: Species,
        level: Int,
        aspects: List<String>,
        form: String?,
        shiny: Boolean?
    ): Pokemon? {
        val normalizedSpeciesId = normalizeSpeciesId(species.resourceIdentifier.toString()) ?: return null
        val tokenVariants = listOf(
            normalizedAspectProperties(aspects, false),
            normalizedAspectProperties(aspects, true)
        )

        for (tokens in tokenVariants) {
            val configuredTokens = tokens.toMutableList()
            if (!form.isNullOrBlank()) {
                configuredTokens.add("form=${form.trim().lowercase(Locale.ROOT)}")
            }
            if (shiny != null) {
                configuredTokens.add("shiny=$shiny")
            }

            val joined = configuredTokens.joinToString(" ")
            val candidates = listOf(
                "$normalizedSpeciesId level=${level.coerceAtLeast(1)}${if (joined.isBlank()) "" else " $joined"}".trim(),
                "species=\"$normalizedSpeciesId\" level=${level.coerceAtLeast(1)}${if (joined.isBlank()) "" else " $joined"}".trim()
            )

            for (candidate in candidates) {
                try {
                    val pokemon = PokemonProperties.parse(candidate).create()
                    if (pokemon != null && configuredCandidateAppliedRequestedAspects(configuredTokens, pokemon)) {
                        pokemon.initialize()
                        return pokemon
                    }
                } catch (exception: Exception) {
                    LOGGER.debug("Failed to create configured Pokemon from '{}'", candidate, exception)
                }
            }
        }

        return null
    }

    private fun normalizedAspectProperties(aspects: List<String>, booleanizeCustomAspects: Boolean): List<String> {
        val normalized = mutableListOf<String>()
        for (raw in aspects) {
            if (raw.isBlank()) {
                continue
            }
            normalized.add(normalizeAspect(raw, booleanizeCustomAspects))
        }
        return normalized
    }

    private fun aspectCandidates(raw: String): List<String> {
        val normalized = raw.trim().lowercase(Locale.ROOT)
        if (normalized == "shiny") {
            return listOf("shiny=true")
        }
        if (normalized.startsWith("form:")) {
            return listOf("form=${normalized.substring("form:".length)}")
        }
        if (normalized.contains("=")) {
            val candidates = mutableListOf(normalized)
            val parts = normalized.split("=", limit = 2)
            if (parts.size == 2 && (parts[1] == "true" || parts[1] == "yes") && !isKnownBooleanProperty(parts[0])) {
                candidates.add(parts[0])
            }
            return candidates
        }

        return buildList {
            add(normalized)
            if (!isKnownBareProperty(normalized)) {
                add("$normalized=true")
            }
        }
    }

    private fun normalizeAspect(raw: String, booleanizeCustomAspects: Boolean): String {
        val normalized = raw.trim().lowercase(Locale.ROOT)
        if (normalized == "shiny") {
            return "shiny=true"
        }
        if (normalized.startsWith("form:")) {
            return "form=${normalized.substring("form:".length)}"
        }
        if (normalized.contains("=")) {
            return normalized
        }
        return if (booleanizeCustomAspects) "$normalized=true" else normalized
    }

    private fun configuredCandidateAppliedRequestedAspects(tokens: List<String>, pokemon: Pokemon): Boolean {
        if (tokens.isEmpty()) {
            return true
        }

        for (token in tokens) {
            try {
                val parsed = PokemonProperties.parse(token)
                if (!isMeaningfulAspect(parsed, token)) {
                    continue
                }
                if (!pokemon.aspects.containsAll(parsed.aspects)) {
                    return false
                }
            } catch (_: Exception) {
                return false
            }
        }
        return true
    }

    private fun isMeaningfulAspect(parsed: PokemonProperties, property: String): Boolean {
        return parsed.aspects.isNotEmpty()
            || parsed.form != null
            || parsed.shiny != null
            || isKnownBooleanProperty(property)
    }

    private fun isKnownBooleanProperty(property: String): Boolean {
        return when (property) {
            "shiny", "shiny=true", "shiny=yes", "galarian", "galarian=true", "alolan", "alolan=true",
            "hisuian", "hisuian=true", "paldean", "paldean=true" -> true
            else -> false
        }
    }

    private fun isKnownBareProperty(property: String): Boolean {
        return when (property) {
            "shiny", "galarian", "alolan", "hisuian", "paldean" -> true
            else -> false
        }
    }

    private fun customAspectToken(raw: String): String {
        val normalized = raw.trim().lowercase(Locale.ROOT)
        if (normalized.contains("=")) {
            val parts = normalized.split("=", limit = 2)
            if (parts.size == 2 && (parts[1] == "true" || parts[1] == "yes")) {
                return parts[0]
            }
        }
        return normalized
    }

    private fun normalizeSpeciesId(speciesId: String?): String? {
        if (speciesId.isNullOrBlank()) {
            return null
        }
        return speciesId.trim().lowercase(Locale.ROOT)
    }

    @Suppress("UNCHECKED_CAST")
    private fun replaceAspects(pokemon: Pokemon, aspects: Set<String>) {
        try {
            val getAspects = pokemon.javaClass.getMethod("getAspects")
            val current = getAspects.invoke(pokemon) as? MutableSet<String> ?: return
            current.clear()
            current.addAll(aspects)
        } catch (exception: Exception) {
            LOGGER.debug("Failed to replace aspects for {}", pokemon.species.name, exception)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun mergeAspects(pokemon: Pokemon, aspects: Set<String>) {
        try {
            val getAspects = pokemon.javaClass.getMethod("getAspects")
            val current = getAspects.invoke(pokemon) as? MutableSet<String> ?: return
            current.addAll(aspects)
        } catch (exception: Exception) {
            LOGGER.debug("Failed to merge aspects for {}", pokemon.species.name, exception)
        }
    }
}
