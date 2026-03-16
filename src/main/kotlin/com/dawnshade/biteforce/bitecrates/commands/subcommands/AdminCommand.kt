package com.dawnshade.biteforce.bitecrates.commands.subcommands

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.mojang.brigadier.tree.LiteralCommandNode
import com.dawnshade.biteforce.bitecrates.config.ConfigManager
import com.dawnshade.biteforce.bitecrates.core.BiteCrates
import com.dawnshade.biteforce.bitecrates.data.Crate
import com.dawnshade.biteforce.bitecrates.util.SubCommand
import me.lucko.fabric.api.permissions.v0.Permissions
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.server.level.ServerPlayer
import java.util.concurrent.CompletableFuture

class AdminCommand : SubCommand {
    override fun build(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("admin")
            .requires(Permissions.require("${BiteCrates.MOD_ID}.command.admin", 2))
            .then(
                Commands.literal("give")
                    .requires(Permissions.require("${BiteCrates.MOD_ID}.command.admin.give", 2))
                    .then(
                        Commands.argument("user", EntityArgument.player())
                            .then(
                                Commands.argument("crate", StringArgumentType.word())
                                    .suggests { _, builder ->
                                        ConfigManager.CRATES.keys.forEach(builder::suggest)
                                        builder.buildFuture()
                                    }
                                    .then(
                                        Commands.argument("reward", StringArgumentType.word())
                                            .suggests(::suggestRewards)
                                            .executes { ctx ->
                                                execute(
                                                    ctx,
                                                    EntityArgument.getPlayer(ctx, "user"),
                                                    StringArgumentType.getString(ctx, "crate"),
                                                    StringArgumentType.getString(ctx, "reward")
                                                )
                                            }
                                    )
                            )
                    )
            )
            .build()
    }

    private fun suggestRewards(
        ctx: CommandContext<CommandSourceStack>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val crateId = runCatching { StringArgumentType.getString(ctx, "crate") }.getOrNull()
        val crate = crateId?.let(ConfigManager.CRATES::get)

        builder.suggest("all")
        crate?.rewards?.keys?.forEach(builder::suggest)

        return builder.buildFuture()
    }

    companion object {
        fun execute(
            ctx: CommandContext<CommandSourceStack>,
            player: ServerPlayer,
            crateId: String,
            rewardId: String
        ): Int {
            val crate = ConfigManager.CRATES[crateId] ?: run {
                ctx.source.sendMessage(Component.text("Crate $crateId could not be found!", NamedTextColor.RED))
                return 0
            }

            if (rewardId.equals("all", true)) {
                return giveAllRewards(ctx, player, crate)
            }

            val reward = crate.rewards[rewardId] ?: run {
                ctx.source.sendMessage(
                    Component.text("Reward $rewardId could not be found in crate $crateId!", NamedTextColor.RED)
                )
                return 0
            }

            return if (reward.giveReward(player, crate)) {
                ctx.source.sendMessage(
                    Component.text("Gave reward $rewardId from crate $crateId to ${player.name.string}.", NamedTextColor.GREEN)
                )
                1
            } else {
                ctx.source.sendMessage(
                    Component.text("Failed to give reward $rewardId from crate $crateId to ${player.name.string}.", NamedTextColor.RED)
                )
                0
            }
        }

        private fun giveAllRewards(
            ctx: CommandContext<CommandSourceStack>,
            player: ServerPlayer,
            crate: Crate
        ): Int {
            if (crate.rewards.isEmpty()) {
                ctx.source.sendMessage(
                    Component.text("Crate ${crate.id} has no rewards to give.", NamedTextColor.RED)
                )
                return 0
            }

            val results = crate.rewards.values.associateWith { reward ->
                reward.giveReward(player, crate)
            }
            val successCount = results.count { it.value }

            return when (successCount) {
                0 -> {
                    ctx.source.sendMessage(
                        Component.text("Failed to give any rewards from crate ${crate.id} to ${player.name.string}.", NamedTextColor.RED)
                    )
                    0
                }
                crate.rewards.size -> {
                    ctx.source.sendMessage(
                        Component.text("Gave all ${crate.rewards.size} rewards from crate ${crate.id} to ${player.name.string}.", NamedTextColor.GREEN)
                    )
                    1
                }
                else -> {
                    val failedRewards = results
                        .filterValues { !it }
                        .keys
                        .joinToString(", ") { it.id }
                    ctx.source.sendMessage(
                        Component.text(
                            "Gave $successCount/${crate.rewards.size} rewards from crate ${crate.id} to ${player.name.string}. Failed: $failedRewards",
                            NamedTextColor.YELLOW
                        )
                    )
                    1
                }
            }
        }
    }
}
