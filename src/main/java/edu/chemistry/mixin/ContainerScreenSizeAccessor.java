package edu.chemistry.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** lets a station menu change its size when it switches to the console layout */
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenSizeAccessor {
    @Mutable
    @Accessor("imageWidth")
    void edu$setImageWidth(int width);

    @Mutable
    @Accessor("imageHeight")
    void edu$setImageHeight(int height);
}
