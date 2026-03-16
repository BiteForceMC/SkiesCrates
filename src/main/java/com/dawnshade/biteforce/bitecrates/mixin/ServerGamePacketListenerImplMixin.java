package com.dawnshade.biteforce.bitecrates.mixin;

import com.dawnshade.biteforce.bitecrates.events.ItemSwingEvent;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleAnimate", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V", shift = At.Shift.AFTER))
    private void bitecrates$onHandleAnimate(ServerboundSwingPacket packet, CallbackInfo ci) {
        var itemStack = this.player.getItemInHand(packet.getHand());
        if (itemStack.isEmpty()) return;
        var result = this.player.pick(player.blockInteractionRange(), 1.0F, false);
        if (!(result instanceof BlockHitResult) || result.getType() != HitResult.Type.MISS) {
            return;
        }

        ItemSwingEvent.EVENT.invoker().interact(player, itemStack, packet.getHand());
    }
}
