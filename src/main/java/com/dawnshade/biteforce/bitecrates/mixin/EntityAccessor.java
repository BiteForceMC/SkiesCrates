package com.dawnshade.biteforce.bitecrates.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

@Mixin(Entity.class)
public interface EntityAccessor {
    @Accessor("DATA_NO_GRAVITY")
    static EntityDataAccessor<Boolean> getNoGravity() {
        throw new AssertionError();
    }

    @Accessor("DATA_CUSTOM_NAME")
    static EntityDataAccessor<Optional<Component>> getCustomName() {
        throw new AssertionError();
    }

    @Accessor("DATA_CUSTOM_NAME_VISIBLE")
    static EntityDataAccessor<Boolean> getCustomNameVisible() {
        throw new AssertionError();
    }
}
