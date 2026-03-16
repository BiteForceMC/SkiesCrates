package com.dawnshade.biteforce.bitecrates.data.actions.types

import com.dawnshade.biteforce.bitecrates.data.actions.Action
import com.dawnshade.biteforce.bitecrates.data.actions.ActionType
import com.dawnshade.biteforce.bitecrates.util.Utils
import eu.pb4.sgui.api.gui.SimpleGui
import net.minecraft.server.level.ServerPlayer

class CloseGUI(
) : Action(ActionType.CLOSE_GUI) {
    override fun executeAction(player: ServerPlayer, gui: SimpleGui) {
        Utils.printDebug("[ACTION - ${type.name}] Player(${player.gameProfile.name}): $this")
        gui.close()
    }

    override fun toString(): String {
        return "CloseGUI()"
    }
}
