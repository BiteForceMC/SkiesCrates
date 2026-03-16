package com.dawnshade.biteforce.bitecrates.mixin;

import dev.furq.holodisplays.handlers.ViewerHandler;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ViewerHandler.class)
public interface ViewerHandlerAccessor {
    @Invoker("removeViewer")
    void invokeRemoveViewer(ServerPlayer player, String name);
}
