package edu.chemistry.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.SubtitleOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** lets the control tips find the closed captions, to stay out of their way */
@Mixin(Gui.class)
public interface GuiAccessor {
    @Accessor("subtitleOverlay")
    SubtitleOverlay edu$subtitleOverlay();
}
