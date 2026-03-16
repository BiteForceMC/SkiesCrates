package com.dawnshade.biteforce.bitecrates.placeholders

import com.dawnshade.biteforce.bitecrates.placeholders.services.DefaultPlaceholderService
import com.dawnshade.biteforce.bitecrates.placeholders.services.ImpactorPlaceholderService
import com.dawnshade.biteforce.bitecrates.placeholders.services.MiniPlaceholdersService
import com.dawnshade.biteforce.bitecrates.placeholders.services.PlaceholderAPIService
import com.dawnshade.biteforce.bitecrates.placeholders.type.player.PlayerKeys
import net.minecraft.server.level.ServerPlayer
import java.util.stream.Stream

object PlaceholderManager {
    private val services: MutableList<IPlaceholderService> = mutableListOf()

    fun init() {
        services.add(DefaultPlaceholderService())
        for (service in PlaceholderMods.entries) {
            if (service.isModPresent()) {
                services.add(getServiceForType(service))
            }
        }
        registerPlaceholders()
    }

    private fun registerPlaceholders() {
        




        
        Stream.of(
            PlayerKeys()
        ).forEach { placeholder -> services.forEach { it.registerPlayer(placeholder) } }

        services.forEach { it.finalizeRegister() }
    }

    fun parse(player: ServerPlayer, text: String, additionalPlaceholders: Map<String, String> = emptyMap()): String {
        var returnValue = text.let {
            additionalPlaceholders.entries.fold(it) { acc, (key, value) ->
                acc.replace(key, value)
            }
        }
        for (service in services) {
            returnValue = service.parsePlaceholders(player, returnValue)
        }
        return returnValue
    }

    private fun getServiceForType(placeholderMod: PlaceholderMods): IPlaceholderService {
        return when (placeholderMod) {
            PlaceholderMods.IMPACTOR -> ImpactorPlaceholderService()
            PlaceholderMods.PLACEHOLDERAPI -> PlaceholderAPIService()
            PlaceholderMods.MINIPLACEHOLDERS -> MiniPlaceholdersService()
        }
    }
}
