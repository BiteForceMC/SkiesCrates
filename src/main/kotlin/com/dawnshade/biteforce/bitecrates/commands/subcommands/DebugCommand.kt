package com.dawnshade.biteforce.bitecrates.commands.subcommands

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import com.dawnshade.biteforce.bitecrates.core.BiteCrates
import com.dawnshade.biteforce.bitecrates.config.ConfigManager
import com.dawnshade.biteforce.bitecrates.util.SubCommand
import me.lucko.fabric.api.permissions.v0.Permissions
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands

class DebugCommand : SubCommand {
    override fun build(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("debug")
            .requires(Permissions.require("${BiteCrates.MOD_ID}.command.debug", 2))
            .executes(Companion::debug)
            .build()
    }

    companion object {
        fun debug(ctx: CommandContext<CommandSourceStack>): Int {
            val newMode = !ConfigManager.CONFIG.debug
            ConfigManager.CONFIG.debug = newMode
            ConfigManager.saveFile("config.json", ConfigManager.CONFIG)

            ctx.source.sendMessage(
                if (newMode)
                    Component.text("Debug mode has been enabled!").color(NamedTextColor.GREEN)
                else
                    Component.text("Debug mode has been disabled!", NamedTextColor.RED)
            )
            return 1
        }
    }
}
