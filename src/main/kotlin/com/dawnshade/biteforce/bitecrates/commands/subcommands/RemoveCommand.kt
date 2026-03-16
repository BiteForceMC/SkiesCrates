package com.dawnshade.biteforce.bitecrates.commands.subcommands

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import com.dawnshade.biteforce.bitecrates.core.BiteCrates
import com.dawnshade.biteforce.bitecrates.config.ConfigManager
import com.dawnshade.biteforce.bitecrates.data.DimensionalBlockPos
import com.dawnshade.biteforce.bitecrates.core.CratesManager
import com.dawnshade.biteforce.bitecrates.util.SubCommand
import com.dawnshade.biteforce.bitecrates.util.TextUtils
import me.lucko.fabric.api.permissions.v0.Permissions
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.world.phys.BlockHitResult

class RemoveCommand : SubCommand {
    override fun build(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("remove")
            .requires(Permissions.require("${BiteCrates.MOD_ID}.command.remove", 2))
            .executes { ctx: CommandContext<CommandSourceStack> ->
                execute(ctx)
            }
            .build()
    }

    companion object {
        fun execute(
            ctx: CommandContext<CommandSourceStack>
        ): Int {
            val player = ctx.source.playerOrException

            val blockResult = player.pick(5.0, 1.0F, false)
            if (blockResult == null || blockResult !is BlockHitResult) {
                ctx.source.sendMessage(Component.text("You must be looking at a block to remove a crate!", NamedTextColor.RED))
                return 0
            }

            val state = player.serverLevel().getBlockState(blockResult.blockPos)
            if (state.isAir) {
                ctx.source.sendMessage(Component.text("You must be looking at a valid block to remove a crate!", NamedTextColor.RED))
                return 0
            }

            val dimPos = DimensionalBlockPos(
                player.serverLevel().dimension().location().asString(),
                blockResult.blockPos.x,
                blockResult.blockPos.y,
                blockResult.blockPos.z
            )

            val crateInstances = ConfigManager.CRATES.filter { (_, crate) ->
                crate.block.locations.any { it.equalsDimBlockPos(dimPos) }
            }
            if (crateInstances.isEmpty()) {
                ctx.source.sendMessage(TextUtils.toComponent("<red>This block is not set as any active crate!"))
                return 0
            }

            
            CratesManager.getCrateFromPos(dimPos)?.let {
                CratesManager.unloadCrateLocation(it)
            }

            var removed = 0
            for ((_, crate) in crateInstances) {
                crate.block.locations.removeAll { crateLoc ->
                    crateLoc.equalsDimBlockPos(dimPos)
                }
                if (!ConfigManager.saveFile("crates/${crate.id}.json", crate)) {
                    ctx.source.sendMessage(Component.text("Failed to save crate data for ${crate.id}! Check the console for additional errors...", NamedTextColor.RED))
                    continue
                }

                removed++
            }

            if (removed == 0) {
                ctx.source.sendMessage(Component.text("Failed to remove crates at the position ${dimPos.x}, ${dimPos.y}, ${dimPos.z}! Check the console for additional errors...", NamedTextColor.RED))
                return 0
            }

            ctx.source.sendMessage(Component.text("Successfully removed $removed crate(s) at the position ${dimPos.x}, ${dimPos.y}, ${dimPos.z}!", NamedTextColor.GREEN))

            return 1
        }
    }
}
