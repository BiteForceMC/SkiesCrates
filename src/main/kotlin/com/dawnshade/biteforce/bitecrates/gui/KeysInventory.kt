package com.dawnshade.biteforce.bitecrates.gui

import com.dawnshade.biteforce.bitecrates.core.BiteCrates
import com.dawnshade.biteforce.bitecrates.config.ConfigManager
import com.dawnshade.biteforce.bitecrates.config.Lang
import com.dawnshade.biteforce.bitecrates.util.TextUtils
import com.dawnshade.biteforce.bitecrates.util.Utils
import eu.pb4.sgui.api.elements.GuiElementBuilder
import eu.pb4.sgui.api.gui.SimpleGui
import net.minecraft.server.level.ServerPlayer

class KeysInventory(viewer: ServerPlayer, private val target: ServerPlayer): SimpleGui(
    ConfigManager.KEYS_MENU.type.type, viewer, false
) {
    private val keysMenu = ConfigManager.KEYS_MENU

    init {
        this.title = TextUtils.parseAllNative(player, keysMenu.title)
        refresh()
    }

    private fun refresh() {
        val storage = BiteCrates.INSTANCE.storage

        keysMenu.items.forEach { (_, item) ->
            item.createItemStack(player).let {
                item.slots.forEach { slot ->
                    this.setSlot(slot, GuiElementBuilder(it)
                        .setCallback { click ->
                            item.actions.forEach { (_, action) ->
                                action.executeAction(player, this)
                            }
                        }
                    )
                }
            }
        }

        storage.getUserAsync(target.uuid).thenAccept { playerData ->
            keysMenu.keys.forEach { (id, item) ->
                val key = ConfigManager.KEYS[id] ?: run {
                    Utils.printError("Key $id could not be found while opening keys menu!")
                    return@forEach
                }
                item.createItemStack(player, key, playerData.keys[id] ?: 0).let {
                    item.slots.forEach { slot ->
                        this.setSlot(slot, it)
                    }
                }
            }
        }.exceptionally {
            Utils.printError("Player data for ${target.name} could not be found while opening keys menu! Is the storage properly initialized?")
            Lang.ERROR_STORAGE.forEach {
                player.sendMessage(TextUtils.toNative(it))
            }
            close()
            return@exceptionally null
        }
    }
}
