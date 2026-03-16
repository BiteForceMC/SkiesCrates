package com.dawnshade.biteforce.bitecrates.data.actions

import com.dawnshade.biteforce.bitecrates.data.actions.types.*

enum class ActionType(val identifier: String, val clazz: Class<*>) {
    COMMAND_CONSOLE("command_console", CommandConsole::class.java),
    COMMAND_PLAYER("command_player", CommandPlayer::class.java),
    MESSAGE("message", MessagePlayer::class.java),
    BROADCAST("broadcast", MessageBroadcast::class.java),
    PLAY_SOUND("play_sound", PlaySound::class.java),
    NEXT_PAGE("next_page", NextPage::class.java),
    PREVIOUS_PAGE("previous_page", PreviousPage::class.java),
    CLOSE_GUI("close_gui", CloseGUI::class.java),;

    companion object {
        fun valueOfAnyCase(name: String): ActionType? {
            for (type in entries) {
                if (name.equals(type.identifier, true)) return type
            }
            return null
        }
    }
}
