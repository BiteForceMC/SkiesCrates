package com.dawnshade.biteforce.bitecrates.mixin;

import com.dawnshade.biteforce.bitecrates.integrations.holodisplays.CrateHologramData;
import com.dawnshade.biteforce.bitecrates.core.HologramsManager;
import dev.furq.holodisplays.handlers.ViewerHandler;
import kotlin.Unit;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ViewerHandler.class)
public class ViewerHandlerMixin {

    @Inject(method = "addViewer", at = @At("HEAD"), cancellable = true)
    private void bitecrates$addViewer(ServerPlayer player, String name, CallbackInfoReturnable<Unit> cir) {
        CrateHologramData data = HologramsManager.INSTANCE.getHologramData(name);
        if (data != null && data.getHiddenPlayers().contains(player.getUUID())) {
            cir.setReturnValue(Unit.INSTANCE);
        }
    }
}
