package com.dawnshade.biteforce.bitecrates.data.actions.types

import com.dawnshade.biteforce.bitecrates.data.actions.Action
import com.dawnshade.biteforce.bitecrates.data.actions.ActionType
import com.dawnshade.biteforce.bitecrates.gui.PreviewInventory
import com.dawnshade.biteforce.bitecrates.util.Utils
import eu.pb4.sgui.api.gui.SimpleGui
import net.minecraft.server.level.ServerPlayer

class NextPage(
) : Action(ActionType.NEXT_PAGE) {
    override fun executeAction(player: ServerPlayer, gui: SimpleGui) {
        Utils.printDebug("[ACTION - ${type.name}] Player(${player.gameProfile.name}) $this")

        if (gui !is PreviewInventory) {
            Utils.printDebug("[ACTION - ${type.name}] Player(${player.gameProfile.name}) tried to execute a NextPage action not paginated.")
            return
        }

        gui.nextPage()
    }

    override fun toString(): String {
        return "NextPage()"
    }
}
