package com.dawnshade.biteforce.bitecrates.data.actions.types

import com.dawnshade.biteforce.bitecrates.data.actions.Action
import com.dawnshade.biteforce.bitecrates.data.actions.ActionType
import com.dawnshade.biteforce.bitecrates.util.Utils
import eu.pb4.sgui.api.gui.SimpleGui
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource

class PlaySound(
    type: ActionType = ActionType.PLAY_SOUND,
    private val sound: String = "",
    private val source: String? = null,
    private val volume: Float = 1.0F,
    private val pitch: Float = 1.0F
) : Action(type) {
    override fun executeAction(player: ServerPlayer, gui: SimpleGui) {
        if (sound.isEmpty()) {
            Utils.printError("[ACTION - ${type.name}] There was an error while executing for player ${player.name}: Sound ID was empty")
            return
        }

        val soundEvent = SoundEvent.createVariableRangeEvent(ResourceLocation.parse(sound))

        var category = if (source == null) SoundSource.MASTER else SoundSource.entries.firstOrNull { it.name.equals(source, true) }
        if (category == null) {
            Utils.printError("[ACTION - ${type.name}] There was an error while executing for player ${player.name}: Sound Source '$source' was not found, defaulting to MASTER")
            category = SoundSource.MASTER
        }

        Utils.printDebug("[ACTION - ${type.name}] Player(${player.gameProfile.name}), SoundEvent($soundEvent), Category($category): $this")

        if (!player.server.isStopped) {
            player.server.executeIfPossible {
                player.playNotifySound(
                    soundEvent,
                    category,
                    volume,
                    pitch,
                )
            }
        }
    }

    override fun toString(): String {
        return "PlaySound(sound='$sound', source=$source, volume=$volume, pitch=$pitch)"
    }
}
