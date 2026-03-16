package com.dawnshade.biteforce.bitecrates.commands.subcommands

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import com.dawnshade.biteforce.bitecrates.core.BiteCrates
import com.dawnshade.biteforce.bitecrates.config.ConfigManager
import com.dawnshade.biteforce.bitecrates.core.CratesManager
import com.dawnshade.biteforce.bitecrates.util.SubCommand
import me.lucko.fabric.api.permissions.v0.Permissions
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.server.level.ServerPlayer

class GiveCommand : SubCommand {
    override fun build(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("give")
            .requires(Permissions.require("${BiteCrates.MOD_ID}.command.give", 2))
            .then(Commands.argument("targets", EntityArgument.players())
                .then(Commands.argument("crate", StringArgumentType.string())
                    .suggests { context, builder ->
                        ConfigManager.CRATES.forEach { builder.suggest(it.key) }
                        builder.buildFuture()
                    }
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .then(Commands.argument("silent", BoolArgumentType.bool())
                            .executes { ctx: CommandContext<CommandSourceStack> ->
                                execute(
                                    ctx,
                                    EntityArgument.getPlayers(ctx, "targets").toList(),
                                    StringArgumentType.getString(ctx, "crate"),
                                    IntegerArgumentType.getInteger(ctx, "amount"),
                                    BoolArgumentType.getBool(ctx, "silent")
                                )
                            }
                        )
                        .executes { ctx: CommandContext<CommandSourceStack> ->
                            execute(
                                ctx,
                                EntityArgument.getPlayers(ctx, "targets").toList(),
                                StringArgumentType.getString(ctx, "crate"),
                                IntegerArgumentType.getInteger(ctx, "amount")
                            )
                        }
                    )
                    .executes { ctx: CommandContext<CommandSourceStack> ->
                        execute(
                            ctx,
                            EntityArgument.getPlayers(ctx, "targets").toList(),
                            StringArgumentType.getString(ctx, "crate")
                        )
                    }
                )
            )
            .build()
    }

    companion object {
        fun execute(
            ctx: CommandContext<CommandSourceStack>,
            targets: List<ServerPlayer>,
            crateId: String,
            amount: Int = 1,
            silent: Boolean = false
        ): Int {
            val crate = ConfigManager.CRATES[crateId] ?: run {
                ctx.source.sendMessage(Component.text("Crate $crateId could not be found!", NamedTextColor.RED))
                return 0
            }

            val results = targets.map { player ->
                CratesManager.giveCrates(crate, player, amount, silent)
            }

            val successful = results.filter { it }.size
            when (successful) {
                0 -> ctx.source.sendMessage(
                    Component.text("Failed to give ${amount}x $crateId crates to any players!", NamedTextColor.RED)
                )
                targets.size -> ctx.source.sendMessage(
                    Component.text("Successfully gave ${amount}x $crateId crates to $successful players!").color(NamedTextColor.GREEN)
                )
                else -> ctx.source.sendMessage(
                    Component.text("Successfully gave ${amount}x $crateId crates to $successful player(s), " +
                            "but failed to give it to ${targets.size - successful} player(s)!").color(NamedTextColor.YELLOW)
                )
            }
            return 1
        }
    }
}
