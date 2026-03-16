package com.dawnshade.biteforce.bitecrates.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Display.TextDisplay.class)
public interface TextDisplayAccessor {
    @Accessor("DATA_TEXT_ID")
    static EntityDataAccessor<Component> getText() {
        throw new AssertionError();
    }

    @Accessor("DATA_LINE_WIDTH_ID")
    static EntityDataAccessor<Integer> getLineWidth() {
        throw new AssertionError();
    }

    @Accessor("DATA_BACKGROUND_COLOR_ID")
    static EntityDataAccessor<Integer> getBackgroundColor() {
        throw new AssertionError();
    }

    @Accessor("DATA_TEXT_OPACITY_ID")
    static EntityDataAccessor<Byte> getTextOpacity() {
        throw new AssertionError();
    }

    @Accessor("DATA_STYLE_FLAGS_ID")
    static EntityDataAccessor<Byte> getStyleFlags() {
        throw new AssertionError();
    }

    @Accessor("FLAG_SHADOW")
    static byte getFlagShadow() {
        throw new AssertionError();
    }
}
